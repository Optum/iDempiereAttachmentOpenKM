package org.optum.erp.dms.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.bind.DatatypeConverter;

import org.compiere.model.MClient;
import org.compiere.model.MStorageProvider;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.optum.erp.search.model.SearchFileRoot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The Class OptumUtil.
 */
public class OptumUtil {
	
	/**
	 * Load data.
	 *
	 * @return true, if successful
	 */
	public boolean loadData() {
		return false;
	} 
	
	/**
	 * Save data to open KM.
	 *
	 * @param data the data
	 * @param fileName the file name
	 * @param url the url
	 * @param path the path
	 * @param encoding the encoding
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String saveDataToOpenKM(byte[] data, String fileName, String url, String path, String encoding)
			throws IOException {
		
		String existingUUID = getExistingUUID(fileName, url, path, encoding);
		if(existingUUID != null && existingUUID.length() > 0) {
			//Do Versioning now
			if(checkOut(existingUUID, url, fileName, encoding) == 204) {
				checkIn(data, fileName, url, path, encoding, existingUUID);
				return existingUUID;
			}
		} else {
			url = url + "/services/rest/document/createSimple";
			
			MediaType mediaTypePNG = MediaType.parse("image/png");
			OkHttpClient client = new OkHttpClient();
			RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
					.addFormDataPart("docPath", path + "/" + fileName)
					.addFormDataPart("content", fileName, RequestBody.create(mediaTypePNG, data)).build();

			Request request = new Request.Builder()

					.header("Accept", "application/json").header("Authorization", "Basic " + encoding).url(url)
					.post(requestBody).build();

			Response response = client.newCall(request).execute();
			String responseHttp = response.body().string();
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			Documents document = mapper.readValue(responseHttp, Documents.class);
			return document.getDocument().get(0).getUuid();
		}
		return existingUUID;
	}

	/**
	 * Check in.
	 *
	 * @param data the data
	 * @param fileName the file name
	 * @param url the url
	 * @param path the path
	 * @param encoding the encoding
	 * @param uuid the uuid
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void checkIn(byte[] data, String fileName, String url, String path, String encoding, String uuid) throws IOException{
		url = url + "/services/rest/document/checkin";
		
		MediaType mediaTypePNG = MediaType.parse("image/png");
		OkHttpClient client = new OkHttpClient();
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("docId", uuid)
				.addFormDataPart("content", fileName, RequestBody.create(mediaTypePNG, data)).build();

		Request request = new Request.Builder()

				.header("Accept", "application/json").header("Authorization", "Basic " + encoding).url(url)
				.post(requestBody).build();

		Response response = client.newCall(request).execute();
	}

	/**
	 * Check out.
	 *
	 * @param existingUUID the existing UUID
	 * @param url the url
	 * @param fileName the file name
	 * @param encoding the encoding
	 * @return the int
	 */
	private int checkOut(String existingUUID, String url, String fileName, String encoding) {
		int statusCode = 0;
		try {
			URL obj;
			url = url + "/services/rest/document/checkout?docId="+existingUUID;
	    	obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + encoding);
			con.setRequestProperty("Accept", "application/json");
			statusCode = con.getResponseCode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return statusCode;
	}

	/**
	 * Gets the existing UUID.
	 *
	 * @param fileName the file name
	 * @param url the url
	 * @param path the path
	 * @param encoding the encoding
	 * @return the existing UUID
	 */
	private String getExistingUUID(String fileName, String url, String path, String encoding) {
		
		URL obj;
		try {
			String modifiedFileName = fileName.replaceAll(" ", "%20");
			url = url + "/services/rest/search/findByName?name="+modifiedFileName;
	    	obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + encoding);
			con.setRequestProperty("Accept", "application/json");
			
			InputStream inputStream = con.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

			StringBuilder response = new StringBuilder();
			String currentLine;

			  while ((currentLine = in.readLine()) != null) 
			        response.append(currentLine);
			
			  
			 	ObjectMapper mapper = new ObjectMapper();
			 	mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
				SearchFileRoot root = mapper.readValue(response.toString(), SearchFileRoot.class);
				if(root != null && root.getQueryResults() != null && root.getQueryResults().getQueryResult() != null) {
					for(int i=0;i<root.getQueryResults().getQueryResult().size();i++) {
						if((path + "/" + fileName).toUpperCase().equals(root.getQueryResults().getQueryResult().get(i).getDocument().getPath().toUpperCase())) {
							return root.getQueryResults().getQueryResult().get(i).getDocument().getUuid();
						}
					}
				}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; 
	
	}

	/**
	 * Gets the dmsuuid.
	 *
	 * @param data the data
	 * @param config the config
	 * @param dataTitle the data title
	 * @return the dmsuuid
	 */
	public byte[] getDMSUUID(byte[] data, DMSConfig config, String dataTitle) {
		String uuid = null;
		try {
			uuid = new OptumUtil().saveDataToOpenKM(data, dataTitle,
					config.getUrl(), config.getRootFolder(), config.getEncoding());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return uuid.getBytes();
	}

	/**
	 * Gets the DMS config.
	 *
	 * @param prov the prov
	 * @param tableId the table id
	 * @param recordId the record id
	 * @return the DMS config
	 */
	public static DMSConfig getDMSConfig(MStorageProvider prov, int tableId, int recordId) {
		DMSConfig config = new DMSConfig();
		String auth = prov.getUserName() + ":" + prov.getPassword();
		try {
			config.setEncoding(DatatypeConverter.printBase64Binary(auth.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		config.setUrl(prov.getURL());
		config.setRootFolder(prov.getFolder()); 
		config.setTableName(getTableName(tableId));
		config.setRecordId(recordId);
		return config;
	}
	
	/**
	 * Gets the table name.
	 *
	 * @param tableId the table id
	 * @return the table name
	 */
	public static String getTableName(int tableId) {
		return new MTable(Env.getCtx(), tableId, null).getTableName().toUpperCase();
	}
	
	
	/**
	 * Gets the client name.
	 *
	 * @param clientId the client id
	 * @return the client name
	 */
	public static String getClientName(int clientId) {
		return new MClient(Env.getCtx(), clientId, null).getName();
	}
	
	/**
	 * Gets the document.
	 *
	 * @param uuid the uuid
	 * @param config the config
	 * @return the document
	 */
	public static byte[] getDocument(String uuid, DMSConfig config) {
		byte[] dataEntry = null;
		
		String url = config.getUrl() + "/services/rest/document/getContent?docId="+uuid;
		
		URL obj;
		try {
			obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + config.getEncoding());
			int responseCode = con.getResponseCode();
		    InputStream inputStream;
		    if (200 <= responseCode && responseCode <= 299) {
		        inputStream = con.getInputStream();
		    } else {
		        inputStream = con.getErrorStream();
		    }
 
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
		    int n;

		    while ( (n = inputStream.read(byteChunk)) > 0 ) {
		      baos.write(byteChunk, 0, n);
		    }

		    dataEntry = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataEntry;
	}
	
	/**
	 * Checks if is docs delete by UUID.
	 *
	 * @param uuid the uuid
	 * @param config the config
	 * @return true, if is docs delete by UUID
	 */
	public static boolean isDocsDeleteByUUID(String uuid, DMSConfig config) {
		boolean status = Boolean.TRUE;
		String url = config.getUrl() + "/services/rest/document/delete?docId="+uuid;
		
		URL obj;
		try {
			obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 		con.setRequestMethod("DELETE");
			con.setRequestProperty("Authorization", "Basic " + config.getEncoding());
			int responseCode = con.getResponseCode();
			System.out.println(responseCode);
		} catch (Exception e) {
			status = Boolean.FALSE;
		}
		
		return status;
	}
	
	/**
	 * Gets the client path.
	 *
	 * @return the client path
	 */
	public static String getClientPath() { 
		return "/" + getClientName(Env.getAD_Client_ID(Env.getCtx()));
	}
}