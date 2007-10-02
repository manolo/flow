package com.itmill.toolkit.terminal.gwt.client.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
import com.itmill.toolkit.terminal.gwt.client.Paintable;
import com.itmill.toolkit.terminal.gwt.client.UIDL;

public class ISlider extends Widget implements Paintable {

	public static final String CLASSNAME = "i-slider";

	ApplicationConnection client;

	String id;

	private boolean immediate;
	private boolean disabled;
	private boolean readonly;

	private int handleSize;
	private double min;
	private double max;
	private int resolution;
	private Double value;
	private boolean vertical;
	private int size = -1;
	private boolean arrows;

	/* DOM element for slider's base */
	private Element base;

	/* DOM element for slider's handle */
	private Element handle;

	/* DOM element for decrement arrow */
	private Element smaller;

	/* DOM element for increment arrow */
	private Element bigger;

	/* Temporary dragging/animation variables */
	private boolean dragging = false;
	private Timer anim;

	public ISlider() {
		super();

		setElement(DOM.createDiv());
		base = DOM.createDiv();
		handle = DOM.createDiv();
		smaller = DOM.createDiv();
		bigger = DOM.createDiv();

		setStyleName(CLASSNAME);
		DOM.setElementProperty(base, "className", CLASSNAME + "-base");
		DOM.setElementProperty(handle, "className", CLASSNAME + "-handle");
		DOM.setElementProperty(smaller, "className", CLASSNAME + "-smaller");
		DOM.setElementProperty(bigger, "className", CLASSNAME + "-bigger");

		DOM.appendChild(getElement(), bigger);
		DOM.appendChild(getElement(), smaller);
		DOM.appendChild(getElement(), base);
		DOM.appendChild(base, handle);

		// Hide initially
		DOM.setStyleAttribute(smaller, "display", "none");
		DOM.setStyleAttribute(bigger, "display", "none");
		DOM.setStyleAttribute(handle, "visibility", "hidden");

		DOM.sinkEvents(base, Event.ONMOUSEDOWN);
		DOM.sinkEvents(handle, Event.MOUSEEVENTS);
		DOM.sinkEvents(smaller, Event.ONMOUSEDOWN | Event.ONMOUSEUP);
		DOM.sinkEvents(bigger, Event.ONMOUSEDOWN | Event.ONMOUSEUP);
	}

	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {

		this.client = client;
		this.id = uidl.getId();

		// Ensure correct implementation (handle own caption)
		if (client.updateComponent(this, uidl, false))
			return;

		immediate = uidl.getBooleanAttribute("immediate");
		disabled = uidl.getBooleanAttribute("disabled");
		readonly = uidl.getBooleanAttribute("readonly");

		vertical = uidl.hasAttribute("vertical");
		arrows = uidl.hasAttribute("arrows");

		if (arrows) {
			DOM.setStyleAttribute(smaller, "display", "block");
			DOM.setStyleAttribute(bigger, "display", "block");
			if (vertical) {
				int arrowSize = Integer.parseInt(DOM.getElementProperty(
						smaller, "offsetWidth"));
				DOM.setStyleAttribute(bigger, "marginLeft", arrowSize + "px");
				DOM.setStyleAttribute(bigger, "marginRight", arrowSize + "px");
			}
		}

		if (vertical)
			addStyleName(CLASSNAME + "-vertical");
		else
			removeStyleName(CLASSNAME + "-vertical");

		min = uidl.getDoubleAttribute("min");
		max = uidl.getDoubleAttribute("max");
		resolution = uidl.getIntAttribute("resolution");
		value = new Double(uidl.getDoubleVariable("value"));

		handleSize = uidl.getIntAttribute("hsize");

		if (uidl.hasAttribute("size"))
			size = uidl.getIntAttribute("size");

		buildBase();

		if (!vertical) {
			// Draw handle with a delay to allow base to gain maximum width
			// TODO implement with onLoad or DeferredCommand ??
			Timer delay = new Timer() {
				public void run() {
					buildHandle();
					setValue(value, false, false);
				}
			};
			delay.schedule(100);
		} else {
			buildHandle();
			setValue(value, false, false);
		}
	}

	private void buildBase() {
		if (vertical) {
			// TODO
		} else {
			if (size > -1)
				DOM.setStyleAttribute(getElement(), "width", size + "px");
			else {
				Element p = DOM.getParent(getElement());
				if (Integer.parseInt(DOM.getElementProperty(p, "offsetWidth")) > 50)
					DOM.setStyleAttribute(getElement(), "width", "auto");
				else {
					// Set minimum of 50px width and adjust after all
					// components have (supposedly) been drawn completely.
					DOM.setStyleAttribute(getElement(), "width", "50px");
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							Element p = DOM.getParent(getElement());
							if (Integer.parseInt(DOM.getElementProperty(p,
									"offsetWidth")) > 50)
								DOM.setStyleAttribute(getElement(), "width",
										"auto");
						}
					});
				}
			}
		}
		// Allow absolute positioning of handle
		DOM.setStyleAttribute(base, "position", "relative");

		// TODO attach listeners for focusing and arrow keys
	}

	private void buildHandle() {
		// Allow absolute positioning
		DOM.setStyleAttribute(handle, "position", "absolute");

		if (vertical) {
			// TODO
		} else {
			int t = Integer.parseInt(DOM.getElementProperty(base,
					"offsetHeight"))
					- Integer.parseInt(DOM.getElementProperty(handle,
							"offsetHeight"));
			DOM.setStyleAttribute(handle, "top", (t / 2) + "px");
			DOM.setStyleAttribute(handle, "left", "0px");
			int w = (int) (Double.parseDouble(DOM.getElementProperty(base,
					"offsetWidth")) / 100 * handleSize);
			if (handleSize == -1) {
				int baseW = Integer.parseInt(DOM.getElementProperty(base,
						"offsetWidth"));
				double range = (max - min) * (resolution + 1) * 3;
				w = (int) (baseW - range);
			}
			if (w < 3)
				w = 3;
			DOM.setStyleAttribute(handle, "width", w + "px");
		}

		DOM.setStyleAttribute(handle, "visibility", "visible");

	}

	private void setValue(Double value, boolean animate, boolean updateToServer) {
		if (vertical) {
			// TODO
		} else {
			int handleWidth = Integer.parseInt(DOM.getElementProperty(handle,
					"offsetWidth"));
			int baseWidth = Integer.parseInt(DOM.getElementProperty(base,
					"offsetWidth"));
			int range = baseWidth - handleWidth;
			double v = value.doubleValue();
			double valueRange = max - min;
			double p = 0;
			if (valueRange != 0)
				p = range * ((v - min) / valueRange);
			if (p < 0)
				p = 0;
			final double pos = p;

			String styleLeft = DOM.getStyleAttribute(handle, "left");
			int left = Integer.parseInt(styleLeft.substring(0, styleLeft
					.length() - 2));

			if ((int) (Math.round(pos)) != left && animate) {
				if (anim != null)
					anim.cancel();
				anim = new Timer() {
					private int left;
					private int goal = (int) Math.round(pos);
					private int dir = 0;

					public void run() {
						String styleLeft = DOM
								.getStyleAttribute(handle, "left");
						left = Integer.parseInt(styleLeft.substring(0,
								styleLeft.length() - 2));

						// Determine direction
						if (dir == 0)
							dir = (goal - left) / Math.abs(goal - left);

						if ((dir > 0 && left >= goal)
								|| (dir < 0 && left <= goal)) {
							this.cancel();
							return;
						}
						int increment = (goal - left) / 2;
						DOM.setStyleAttribute(handle, "left",
								(left + increment) + "px");
					}
				};
				anim.scheduleRepeating(50);
			} else
				DOM.setStyleAttribute(handle, "left", ((int) pos) + "px");
			// DOM.setAttribute(handle, "title", ""+v);
		}

		if (value.doubleValue() < min)
			value = new Double(min);
		else if (value.doubleValue() > max)
			value = new Double(max);

		this.value = value;

		if (updateToServer)
			client.updateVariable(id, "value", value.doubleValue(), immediate);
	}

	public void onBrowserEvent(Event event) {
		if (disabled || readonly)
			return;
		Element targ = DOM.eventGetTarget(event);
		if (dragging || DOM.compare(targ, handle)) {
			processHandleEvent(event);

		} else if (DOM.compare(targ, smaller)) {
			// Decrease value by resolution
			if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
				setValue(new Double(value.doubleValue()
						- Math.pow(10, -resolution)), false, true);
				if (anim != null)
					anim.cancel();
				anim = new Timer() {
					public void run() {
						if (value.doubleValue() - Math.pow(10, -resolution) > min)
							setValue(new Double(value.doubleValue()
									- Math.pow(10, -resolution)), false, true);
					}
				};
				anim.scheduleRepeating(100);
				DOM.eventCancelBubble(event, true);
			} else if (DOM.eventGetType(event) == Event.ONMOUSEUP) {
				anim.cancel();
			}

		} else if (DOM.compare(targ, bigger)) {
			// Increase value by resolution
			if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
				setValue(new Double(value.doubleValue()
						+ Math.pow(10, -resolution)), false, true);
				if (anim != null)
					anim.cancel();
				anim = new Timer() {
					public void run() {
						if (value.doubleValue() - Math.pow(10, -resolution) < max)
							setValue(new Double(value.doubleValue()
									+ Math.pow(10, -resolution)), false, true);
					}
				};
				anim.scheduleRepeating(100);
				DOM.eventCancelBubble(event, true);
			} else if (DOM.eventGetType(event) == Event.ONMOUSEUP) {
				anim.cancel();
			}

		} else
			processBaseEvent(event);
	}

	private void processHandleEvent(Event event) {
		switch (DOM.eventGetType(event)) {
		case Event.ONMOUSEDOWN:
			if (!disabled && !readonly) {
				if (anim != null)
					anim.cancel();
				dragging = true;
				DOM.setCapture(getElement());
				DOM.eventPreventDefault(event); // prevent selecting text
				DOM.eventCancelBubble(event, true);
			}
			break;
		case Event.ONMOUSEMOVE:
			if (dragging) {
				DOM.setCapture(getElement());
				setValueByEvent(event, false, false);
			}
			break;
		case Event.ONMOUSEUP:
			dragging = false;
			DOM.releaseCapture(getElement());
			setValueByEvent(event, true, true);
			break;
		default:
			break;
		}
	}

	private void processBaseEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
			if (!disabled && !readonly && !dragging) {
				setValueByEvent(event, true, true);
				DOM.eventCancelBubble(event, true);
			}
		}
	}

	private void setValueByEvent(Event event, boolean animate, boolean roundup) {
		int x = DOM.eventGetClientX(event);
		// int y = DOM.eventGetClientY(event);
		double v = min; // Fallback to min
		if (vertical) {
			// TODO
		} else {
			double handleW = Integer.parseInt(DOM.getElementProperty(handle,
					"offsetWidth"));
			double baseX = DOM.getAbsoluteLeft(base) + handleW / 2;
			double baseW = Integer.parseInt(DOM.getElementProperty(base,
					"offsetWidth"));
			v = ((x - baseX) / (baseW - handleW)) * (max - min) + min;
		}

		if (v < min)
			v = min;
		else if (v > max)
			v = max;

		if (roundup) {
			if (resolution > 0) {
				v = (int) (v * (double) Math.pow(10, resolution));
				v = v / (double) Math.pow(10, resolution);
			} else
				v = Math.round(v);
		}

		setValue(new Double(v), animate, roundup);
	}

}
