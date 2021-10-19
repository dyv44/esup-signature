package org.esupportail.esupsignature.service.interfaces.fs;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.smb.SmbAccessImpl;
import org.esupportail.esupsignature.service.interfaces.fs.vfs.VfsAccessImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FsAccessFactory {


	private SmbAccessImpl smbAccessImpl;

	private VfsAccessImpl vfsAccessImpl;

	private CmisAccessImpl cmisAccessImpl;

	@Autowired(required = false)
	public void setSmbAccessImpl(SmbAccessImpl smbAccessImpl) {
		this.smbAccessImpl = smbAccessImpl;
	}

	@Autowired(required = false)
	public void setVfsAccessImpl(VfsAccessImpl vfsAccessImpl) {
		this.vfsAccessImpl = vfsAccessImpl;
	}

	@Autowired(required = false)
	public void setCmisAccessImpl(CmisAccessImpl cmisAccessImpl) {
		this.cmisAccessImpl = cmisAccessImpl;
	}
	public FsAccessService getFsAccessService(String uri) throws EsupSignatureFsException {
		DocumentIOType type = getPathIOType(uri);
		switch (type) {
			case smb:
				return smbAccessImpl;
			case vfs:
				return vfsAccessImpl;
			case cmis:
				return cmisAccessImpl;
			default:
				return null;
		}
	}

	public List<FsAccessService> getFsAccessServices() {
		List<FsAccessService> fsAccessServices = new ArrayList<>();
		if(smbAccessImpl != null) {
			fsAccessServices.add(smbAccessImpl);
		}
		if(vfsAccessImpl != null) {
			fsAccessServices.add(vfsAccessImpl);
		}
		if(cmisAccessImpl != null) {
			fsAccessServices.add(cmisAccessImpl);
		}
		return fsAccessServices;
	}

	public DocumentIOType getPathIOType(String path) throws EsupSignatureFsException {
		try {
			var uri = new java.net.URI(path);
			switch (uri.getScheme()) {
				case "smb": return DocumentIOType.smb;
				case "cmis": return DocumentIOType.cmis;
				case "file":
				case "sftp":
				case "ftp":
					return DocumentIOType.vfs;
				case "http":
				case "https":
					return DocumentIOType.rest;
			}
			throw new EsupSignatureFsException("unknown protocol for url " + path);
		} catch (java.net.URISyntaxException e) {
			throw new EsupSignatureFsException("target Url error", e);
		}
	}
}
