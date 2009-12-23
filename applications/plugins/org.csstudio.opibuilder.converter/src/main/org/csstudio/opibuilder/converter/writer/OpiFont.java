package org.csstudio.opibuilder.converter.writer;

import org.apache.log4j.Logger;
import org.csstudio.opibuilder.converter.model.EdmFont;
import org.w3c.dom.Element;

/**
 * XML output class for EdmFont type.
 * @author Matevz
 */
public class OpiFont extends OpiAttribute {

	// Definitions copied from org.eclipse.swt.SWT class.
	private static final int NORMAL = 0;
	private static final int BOLD = 1 << 0;
	private static final int ITALIC = 1 << 1;

	private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.writer.OpiFont");	

	/**
	 * Creates an element: 
	 * <tag>
	 *   	 <font fontName="fontNameValue" height="heightValue" style="styleValue" /> 
	 * </tag>
	 * styleValue is determined this way:
	 * 		0 - medium, regular
	 * 		1 - bold, regular
	 * 		2 - medium, italic
	 * 		3 - bold, italic
	 */
	public OpiFont(Context con, String tag, EdmFont f) {
		super(con, tag);

		Element fontElement = context.getDocument().createElement("fontdata");
		context.getElement().appendChild(fontElement);

		String fontName = f.getName();
		String height = String.valueOf(f.getSize()-2);

		// Style conversion copied from org.eclipse.swt.SWT class.
		int s = NORMAL;
		if (f.isBold()) {
			s |= BOLD;
		}
		if (f.isItalic()) {
			s |= ITALIC;
		}
		String style = String.valueOf(s);

		fontElement.setAttribute("fontName", fontName);
		fontElement.setAttribute("height", height);
		fontElement.setAttribute("style", style);

		log.debug("Written font property with attributes: " + fontName + ", " + height + ", " + style);
	}

}
