package org.optum.erp.dms.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.compiere.model.IAttachmentStore;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MStorageProvider;
import org.compiere.util.CLogger;
import org.optum.erp.dms.utils.DMSConfig;
import org.optum.erp.dms.utils.OptumUtil;


/**
 * The Class AttachmentOpenKM.
 */
public class AttachmentOpenKM implements IAttachmentStore {

	/** The log. */
	private final CLogger log = CLogger.getCLogger(getClass());
	
	/** The Constant ZIP. */
	public static final String ZIP = "zip";

	/**
	 * Load LOB data.
	 *
	 * @param attach the attach
	 * @param prov the prov
	 * @return true, if successful
	 */
	@Override
	public boolean loadLOBData(MAttachment attach, MStorageProvider prov) {

		attach.m_items = new ArrayList<MAttachmentEntry>();

		byte[] data = attach.getBinaryData();
		if (data == null)
			return true;
		if (log.isLoggable(Level.FINE))
			log.fine("ZipSize=" + data.length);
		if (data.length == 0)
			return true;

		// Old Format - single file
		if (!ZIP.equals(attach.getTitle())) {
			attach.m_items.add(new MAttachmentEntry(attach.getTitle(), data, 1));
			return true;
		}

		try {
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buffer = new byte[2048];
				int length = zip.read(buffer);
				while (length != -1) {
					out.write(buffer, 0, length);
					length = zip.read(buffer);
				}
				//
				byte[] dataEntry = out.toByteArray();
				if (log.isLoggable(Level.FINE))
					log.fine(name + " - size=" + dataEntry.length + " - zip=" + entry.getCompressedSize() + "("
							+ entry.getSize() + ") " + (entry.getCompressedSize() * 100 / entry.getSize()) + "%");
				//Getting OpenKM configuration
				DMSConfig config = OptumUtil.getDMSConfig(prov, attach.getAD_Table_ID(), attach.getRecord_ID());
				dataEntry = OptumUtil.getDocument(new String(dataEntry), config);
				attach.m_items.add(new MAttachmentEntry(name, dataEntry, attach.m_items.size() + 1));
				entry = zip.getNextEntry();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "loadLOBData", e);
			attach.m_items = null;
			return false;
		}
		return true;
	}

	/**
	 * Save.
	 *
	 * @param attach the attach
	 * @param prov the prov
	 * @return true, if successful
	 */
	//Using this method data will be stored in OpenKM
	@Override
	public boolean save(MAttachment attach, MStorageProvider prov) {

		if (attach.m_items == null || attach.m_items.isEmpty()) {
			attach.setBinaryData(null);
			return true;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(out);
		zip.setMethod(ZipOutputStream.DEFLATED);
		zip.setLevel(Deflater.BEST_COMPRESSION);
		zip.setComment("iDempiere");
		//
		try {
			for (int i = 0; i < attach.m_items.size(); i++) {
				MAttachmentEntry item = attach.getEntry(i);
				ZipEntry entry = new ZipEntry(item.getName());
				entry.setTime(System.currentTimeMillis());
				entry.setMethod(ZipEntry.DEFLATED);
				zip.putNextEntry(entry);
				DMSConfig config = OptumUtil.getDMSConfig(prov, attach.getAD_Table_ID(), attach.getRecord_ID());
				String dataTitle = attach.m_items.get(i).toString();
				byte[] data = new OptumUtil().getDMSUUID(item.getData(), config, dataTitle);
				zip.write(data, 0, data.length);
				zip.closeEntry();
				if (log.isLoggable(Level.FINE))
					log.fine(entry.getName() + " - " + entry.getCompressedSize() + " (" + entry.getSize() + ") "
							+ (entry.getCompressedSize() * 100 / entry.getSize()) + "%");
			}
			zip.close();
			byte[] zipData = out.toByteArray();
			if (log.isLoggable(Level.FINE))
				log.fine("Length=" + zipData.length);
			attach.setBinaryData(zipData);
			attach.setTitle(MAttachment.ZIP);
			return true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "saveLOBData", e);
		}
		attach.setBinaryData(null);
		return false;
	}

	/**
	 * Delete.
	 *
	 * @param attach the attach
	 * @param prov the prov
	 * @return true, if successful
	 */
	//Using this method attachment will be deleted from OpenKM
	@Override
	public boolean delete(MAttachment attach, MStorageProvider prov) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(attach.getBinaryData());
			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buffer = new byte[2048];
				int length = zip.read(buffer);
				while (length != -1) {
					out.write(buffer, 0, length);
					length = zip.read(buffer);
				}
				//
				byte[] dataEntry = out.toByteArray();
				DMSConfig config = OptumUtil.getDMSConfig(prov, attach.getAD_Table_ID(), attach.getRecord_ID());
				OptumUtil.isDocsDeleteByUUID(new String(dataEntry), config);
				entry = zip.getNextEntry();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error While Delete From DMS", e);
			return false;
		}
		return true;
	}

	/**
	 * Delete entry.
	 *
	 * @param attach the attach
	 * @param prov the prov
	 * @param index the index
	 * @return true, if successful
	 */
	//Using this method attachment entry will be deleted from OpenKM
	@Override
	public boolean deleteEntry(MAttachment attach, MStorageProvider prov, int index) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(attach.getBinaryData());
			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {
				if (attach.m_items.get(index).getName().equals(entry.getName())) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int length = zip.read(buffer);
					while (length != -1) {
						out.write(buffer, 0, length);
						length = zip.read(buffer);
					}
					//
					byte[] dataEntry = out.toByteArray();
					DMSConfig config = OptumUtil.getDMSConfig(prov, attach.getAD_Table_ID(), attach.getRecord_ID());
					OptumUtil.isDocsDeleteByUUID(new String(dataEntry), config);
					attach.m_items.remove(index);
					return true;
				}
				entry = zip.getNextEntry();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error While Delete From DMS", e);
			return false;
		}
		return true;
	}

}