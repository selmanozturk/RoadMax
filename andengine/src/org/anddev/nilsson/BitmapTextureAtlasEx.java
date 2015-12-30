/**
 * @author Selman OZTURK
 */

package org.anddev.nilsson;


import android.content.Context;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;


public class BitmapTextureAtlasEx extends BitmapTextureAtlas {
    int widthCounter;

    public BitmapTextureAtlasEx(int width, int height, TextureOptions options) {
        super(width, height, options);
        widthCounter = 0;
    }

    public TextureRegion appendTextureAsset(Context c, String assetPath) {
        TextureRegion region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this, c, assetPath, widthCounter, 0);
        widthCounter += region.getWidth();
        return region;
    }

    public TiledTextureRegion appendTiledAsset(Context c, String assetPath, int tiledColumns, int tiledRows) {
        TiledTextureRegion region = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this, c, assetPath, widthCounter, 0, tiledColumns, tiledRows);
        widthCounter += region.getWidth();
        return region;
    }
}
