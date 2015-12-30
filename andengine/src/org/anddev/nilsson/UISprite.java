/**
 * @author Selman OZTURK
 */

package org.anddev.nilsson;

import org.anddev.andengine.entity.sprite.Sprite;

public class UISprite extends UIElement {

	public Sprite sprite;
	public float scaleX = 1, scaleY= 1;
	
	public UISprite(int containerWidth, int containerHeight, Sprite sprite) {
		super(containerWidth, containerHeight);
 
		this.sprite = sprite;		
		this.sprite.setPosition(actualX, actualY);
		SetScale();
	}
	public void setDimens(float width,float height,float leftMargin,float topMargin){
		this.width=width;
		this.height = height;
		this.leftMargin = leftMargin;
		this.topMargin = topMargin;
	}
	@Override
	public void SetProperties(){		
		super.SetProperties();
		SetScale();
	}

	public void SetScale(){
		sprite.setScaleCenter(0, 0);
				
		if(width != -1){
			scaleX = actualWidth / sprite.getWidth();			
		}
		
		if(height != -1){
			scaleY = actualHeight / sprite.getHeight();			
		}		
		
		sprite.setScale(scaleX, scaleY);
	}

	@Override
	public void SetPosition(float x, float y) {
		sprite.setPosition(x, y);		
	}
}
