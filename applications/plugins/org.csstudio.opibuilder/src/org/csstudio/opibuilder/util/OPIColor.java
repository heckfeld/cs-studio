package org.csstudio.opibuilder.util;

import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**The dedicated color type which supports predefined color name in OPI builder color file.
 * If the color name doesn't exist in the color file, the color value is null.
 * @author Xihui Chen
 *
 */
public class OPIColor implements IAdaptable {

	private static final RGB TRANSPARENT_COLOR = new RGB(253,
					254, 252);

	private String colorName;
	
	private RGB colorValue;
	
	private boolean preDefined;
	
	
	static ImageRegistry imageRegistry = new ImageRegistry();
	
	public OPIColor(String colorName) {
		this.colorName = colorName;
		this.colorValue = ColorService.getInstance().getColor(colorName);
		preDefined = true;
	}
	
	public OPIColor(RGB rgb){
		setColorValue(rgb);
	}
	
	public OPIColor(int red, int green, int blue){
		this(new RGB(red, green, blue));
	}
	
	public OPIColor(String name, RGB rgb) {
		this.colorName = name;
		this.colorValue = rgb;
		preDefined = true;
	}

	public String getColorName() {
		return colorName;
	}
	
	/**
	 * @return the rgb value of the color. null if the predefined color does not exist.
	 */
	public RGB getRGBValue() {
		return colorValue;
	}
	
	/**
	 * @return true if this color is predefined in color file, false otherwise.
	 */
	public boolean isPreDefined() {
		return preDefined;
	}
	
	public void setColorName(String colorName) {
		this.colorName = colorName;
		this.colorValue = ColorService.getInstance().getColor(colorName);
		preDefined = true;
	}
	
	public void setColorValue(RGB rgb) {
		this.colorName = "(" + rgb.red + "," + rgb.green + "," + rgb.blue + ")";
		this.colorValue = rgb;
		preDefined = false;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if(adapter == IWorkbenchAdapter.class)
			return new IWorkbenchAdapter() {
				
				public Object getParent(Object o) {
					return null;
				}
				
				public String getLabel(Object o) {
					return getColorName();
				}
				
				public ImageDescriptor getImageDescriptor(Object object) {
					Image image = imageRegistry.get(getID());
					if(image == null){
						image = createIcon(getRGBValue());
						imageRegistry.put(getID(), image);
					}
						
					return ImageDescriptor.createFromImage(image);
				}
				
				public Object[] getChildren(Object o) {
					return new Object[0];
				}
			};
		
		return null;
	}
	
	/**Get the color image for this color. 
	 * @return
	 */
	public Image getImage(){		
		Image image = imageRegistry.get(getID());
		if(image == null){
			image = createIcon(getRGBValue());
			imageRegistry.put(getID(), image);
		}
		return image;		
	}
	
	
	private String getID(){
		return "OPIBUILDER.COLORPROPERTY.ICON_"  //$NON-NLS-1$
			+colorValue.red+"_"+colorValue.green+"_"+colorValue.blue;  //$NON-NLS-1$ //$NON-NLS-2$
	}
	/**
	 * Creates a small icon using the specified color.
	 * 
	 * @param rgb
	 *            the color
	 * @return an icon
	 */
	private Image createIcon(RGB rgb) {
		if(rgb == null)
			 rgb = CustomMediaFactory.COLOR_BLACK;

		Color color = CustomMediaFactory.getInstance().getColor(rgb);

		// create new graphics context, to draw on
		Image image = new Image(Display.getCurrent(), 16, 16);
		GC gc = new GC(image);

		// draw transparent background
		Color bg = CustomMediaFactory.getInstance().getColor(TRANSPARENT_COLOR);
		gc.setBackground(bg);
		gc.fillRectangle(0, 0, 16, 16);
		// draw icon
		gc.setBackground(color);
		Rectangle r = new Rectangle(1, 4, 14, 9);
		gc.fillRectangle(r);
		gc.setBackground(CustomMediaFactory.getInstance().getColor(0,
						0, 0));
		gc.drawRectangle(r);
		gc.dispose();
		
		
		ImageData imageData = image.getImageData();
		imageData.transparentPixel = imageData.palette.getPixel(TRANSPARENT_COLOR);
		image.dispose();
		return new Image(Display.getCurrent(), imageData);
	}
	
	@Override
	public String toString() {
		return getColorName();
	}
	
}
