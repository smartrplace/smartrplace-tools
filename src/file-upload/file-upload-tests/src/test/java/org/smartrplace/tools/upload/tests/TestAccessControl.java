package org.smartrplace.tools.upload.tests;

import java.security.AccessControlContext;
import java.security.AccessController;

import org.smartrplace.tools.servlet.api.ServletAccessControl;

// dummy implementation
public class TestAccessControl implements ServletAccessControl {

	@Override
	public AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}
}
