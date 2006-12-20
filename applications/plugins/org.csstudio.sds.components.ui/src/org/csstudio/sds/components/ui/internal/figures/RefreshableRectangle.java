package org.csstudio.sds.components.ui.internal.figures;

import java.util.Date;

import org.csstudio.sds.dataconnection.StatisticUtil;
import org.csstudio.sds.ui.figures.IRefreshableFigure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A rectangle figure.
 * 
 * @author Sven Wende
 * 
 */
public final class RefreshableRectangle extends RectangleFigure implements
		IRefreshableFigure {

	/**
	 * The fill grade (0 - 100%).
	 */
	private double _fill = 100.0;

	@Override
	public synchronized void repaint() {
		// TODO Auto-generated method stub
		super.repaint();
	}

	@Override
	public synchronized void revalidate() {
		// TODO Auto-generated method stub
		super.revalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void fillShape(final Graphics graphics) {
		Rectangle bounds = getBounds();

		int newW = (int) Math.round(bounds.width * (getFill() / 100));

		graphics.setBackgroundColor(getBackgroundColor());
		graphics.fillRectangle(getBounds());
		graphics.setBackgroundColor(getForegroundColor());
		graphics.fillRectangle(new Rectangle(bounds.getLocation(),
				new Dimension(newW, bounds.height)));
	}

	/**
	 * {@inheritDoc}
	 */
	public void randomNoiseRefresh() {
		setFill(Math.random() * 100);
		repaint();
	}

	/**
	 * Sets the fill grade.
	 * 
	 * @param fill
	 *            the fill grade.
	 */
	public void setFill(final double fill) {
		_fill = fill;
	}

	/**
	 * Gets the fill grade.
	 * 
	 * @return the fill grade
	 */
	public double getFill() {
		return _fill;
	}
}
