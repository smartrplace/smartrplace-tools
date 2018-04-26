/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * This package has been copied and adapted from the OGEMA util bundle
 */
@XmlSchema(elementFormDefault = XmlNsForm.UNQUALIFIED, namespace = "http://projects/open/2012/UniformProxySystem", xmlns = {
		@XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
		@XmlNs(prefix = "og", namespaceURI = NS_OGEMA_REST),
		@XmlNs(prefix = "xs", namespaceURI = "http://www.w3.org/2001/XMLSchema") })
package org.smartrplace.tools.schedule.management.serialization;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import static org.ogema.serialization.JaxbResource.NS_OGEMA_REST;

