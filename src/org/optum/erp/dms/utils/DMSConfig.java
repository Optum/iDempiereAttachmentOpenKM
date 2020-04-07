package org.optum.erp.dms.utils;

public class DMSConfig {
	private String encoding;;
	private String url;
	private String rootFolder;
	private String tableName;
	private int recordId;
	
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getRootFolder() {
		return rootFolder;
	}
	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public int getRecordId() {
		return recordId;
	}
	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}
	
	
	@Override
	public String toString() {
		return "DMSConfig [encoding=" + encoding + ", url=" + url + ", rootFolder=" + rootFolder + ", tableName="
				+ tableName + ", recordId=" + recordId + "]";
	}
	
}
