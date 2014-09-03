/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Libraries.IO;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.Instantiable.Rendering.ReikaModelledBreakFX;
import Reika.DragonAPI.Interfaces.RenderFetcher;
import Reika.DragonAPI.Interfaces.TextureFetcher;
import Reika.DragonAPI.Libraries.MathSci.ReikaPhysicsHelper;
import Reika.DragonAPI.Libraries.World.ReikaBiomeHelper;

public final class ReikaRenderHelper extends DragonAPICore {

	private static final RenderBlocks rb = new RenderBlocks();

	public static enum RenderDistance {
		FAR(),
		NORMAL(),
		SHORT(),
		TINY();
		private static final RenderDistance[] list = values();
	}

	public static RenderDistance getRenderDistance() {
		float r = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
		if (r > 8) {
			return RenderDistance.FAR;
		}
		else if (r > 4) {
			return RenderDistance.NORMAL;
		}
		else if (r > 2) {
			return RenderDistance.SHORT;
		}
		else {
			return RenderDistance.TINY;
		}
	}

	/** Converts a biome to a color multiplier (for use in things like leaf textures).
	 * Args: World, x, z, material (grass, water, etc), bit */
	public static float biomeToColorMultiplier(World world, int x, int y, int z, String mat, int bit) {
		int[] color = ReikaBiomeHelper.biomeToRGB(world, x, y, z, mat);
		float mult = ReikaColorAPI.RGBtoColorMultiplier(color, bit);
		return mult;
	}

	/** Renders a flat circle in the world. Args: radius, center x,y,z, RGBA, angle step */
	public static void renderCircle(double r, double x, double y, double z, int rgba, int step) {
		//GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Tessellator var5 = Tessellator.instance;
		if (var5.isDrawing)
			var5.draw();
		var5.startDrawing(GL11.GL_LINE_LOOP);
		var5.setColorRGBA_I(rgba, rgba >> 24 & 255);
		for (int i = 0; i < 360; i += step) {
			var5.addVertex(x+r*Math.cos(Math.toRadians(i)), y, z+r*Math.sin(Math.toRadians(i)));
		}
		var5.draw();
		//GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	/** Renders a vertical-plane circle in the world. Args: radius, center x,y,z, RGBA, phi, angle step */
	public static void renderVCircle(double r, double x, double y, double z, int rgba, double phi, int step) {
		//GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Tessellator var5 = Tessellator.instance;
		var5.startDrawing(GL11.GL_LINE_LOOP);
		var5.setColorRGBA_I(rgba, rgba >> 24 & 255);
		for (int i = 0; i < 360; i += step) {
			int sign = 1;
			double h = r*Math.cos(ReikaPhysicsHelper.degToRad(i));
			if (i >= 180)
				sign = -1;
			var5.addVertex(x-Math.sin(Math.toRadians(phi))*(sign)*(Math.sqrt(r*r-h*h)), y+r*Math.cos(Math.toRadians(i)), z+r*Math.sin(Math.toRadians(i))*Math.cos(Math.toRadians(phi)));
		}
		var5.draw();
		//GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	/** Renders a line between two points in the world. Args: Start xyz, End xyz, rgb */
	public static void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, int rgba) {
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Tessellator var5 = Tessellator.instance;
		if (var5.isDrawing)
			var5.draw();
		var5.startDrawing(GL11.GL_LINE_LOOP);
		var5.setColorRGBA_I(rgba, rgba >> 24 & 255);
		var5.addVertex(x1, y1, z1);
		var5.addVertex(x2, y2, z2);
		var5.draw();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	public static void disableLighting() {
		Minecraft.getMinecraft().entityRenderer.disableLightmap(1);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	public static void enableLighting() {
		enableEntityLighting();
		RenderHelper.enableStandardItemLighting();
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	public static void disableEntityLighting() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	public static void enableEntityLighting() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	/** Prepare for drawing primitive geometry by disabling all lighting and textures. Args: Is alpha going to be used */
	public static void prepareGeoDraw(boolean alpha) {
		disableLighting();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if (alpha)
			GL11.glEnable(GL11.GL_BLEND);
	}

	public static void exitGeoDraw() {
		enableLighting();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}

	/** Renders a rectangle in-world. Args: r,g,b,a, Start x,y,z, End x,y,z */
	public static void renderRectangle(int r, int g, int b, int a, double x1, double y1, double z1, double x2, double y2, double z2) {
		Tessellator v5 = Tessellator.instance;
		v5.startDrawingQuads();
		v5.setColorRGBA(r, g, b, a);
		v5.addVertex(x1, y1, z1);
		v5.addVertex(x2, y1, z2);
		v5.addVertex(x2, y2, z2);
		v5.addVertex(x1, y2, z1);
		v5.draw();
	}

	/** Renders break particles for custom-rendered TileEntities. Call this one from BlockDestroyEffects!
	 * Args: Base path (contains TE textures, world, x, y, z, Block, EffectRenderer, Allowed Texture Regions<br><br>
	 *
	 * Explanation of Allowed Regions - Expects a list of size-4 double arrays, whose elements are as follows:<br>
	 * allowed[0]: Lower X-coordinate of allowed region in texture file (left)<br>
	 * allowed[1]: Lower Y-coordinate of allowed region in texture file (top)<br>
	 * allowed[2]: Upper X-coordinate of allowed region in texture file (right)<br>
	 * allowed[3]: Upper Y-coordinate of allowed region in texture file (bottom)<br>
	 *
	 * Note that these are referenced to a whole image, so [0,0,1,1] would be the entire image file.
	 * @Author Reika
	 * */
	public static boolean addModelledBlockParticles(String basedir, World world, int x, int y, int z, Block b, EffectRenderer eff, List<double[]> allowedRegions, Class mod) {
		String name = null;
		if (world.getBlock(x, y, z) == b) {
			TileEntity t = world.getTileEntity(x, y, z);
			if (t instanceof RenderFetcher) {
				RenderFetcher te = (RenderFetcher)t;
				TextureFetcher r = te.getRenderer();
				if (r != null)
					name = r.getImageFileName(te);
			}
		}
		String file = basedir+name;
		for (int i = 0; i < 48; i++) {
			int k = rand.nextInt(allowedRegions.size());
			double[] p = allowedRegions.get(k);
			double px = p[0]+rand.nextDouble()*(p[2]-p[0]);
			double py = p[1]+rand.nextDouble()*(p[3]-p[1]);
			double overx = px+ReikaModelledBreakFX.pw-p[2];
			if (overx > 0)
				px -= overx;
			double overy = py+ReikaModelledBreakFX.pw-p[2];
			if (overy > 0)
				py -= overy;
			eff.addEffect(new ReikaModelledBreakFX(world, x+rand.nextDouble(), y+rand.nextDouble(), z+rand.nextDouble(), -1+rand.nextDouble()*2, 2, -1+rand.nextDouble()*2, b, 0, world.getBlockMetadata(x, y, z), file, px, py, mod));
		}
		return true;
	}

	/** Renders break particles for custom-rendered TileEntities. Call this one from BlockHitEffects!
	 * Args: Base path (contains TE textures, world, MovingObjectPosition, Block, EffectRenderer, Allowed Texture Regions <br>
	 * See addModelledBlockParticles(basedir, world, x, y, z, b, eff, allowedRegions) for explanation of the regions. */
	public static boolean addModelledBlockParticles(String basedir, World world, MovingObjectPosition mov, Block b, EffectRenderer eff, List<double[]> allowedRegions, Class mod) {
		if (mov == null)
			return false;
		int x = mov.blockX;
		int y = mov.blockY;
		int z = mov.blockZ;
		String name = null;
		if (world.getBlock(x, y, z) == b) {
			TileEntity t = world.getTileEntity(x, y, z);
			if (t instanceof RenderFetcher) {
				RenderFetcher te = (RenderFetcher)t;
				TextureFetcher r = te.getRenderer();
				if (r != null)
					name = r.getImageFileName(te);
			}
		}
		int j = 1+rand.nextInt(2);
		String file = basedir+name;
		for (int i = 0; i < j; i++) {
			int k = rand.nextInt(allowedRegions.size());
			double[] p = allowedRegions.get(k);
			double px = p[0]+rand.nextDouble()*(p[2]-p[0]);
			double py = p[1]+rand.nextDouble()*(p[3]-p[1]);
			double overx = px+ReikaModelledBreakFX.pw-p[2];
			if (overx > 0)
				px -= overx;
			double overy = py+ReikaModelledBreakFX.pw-p[2];
			if (overy > 0)
				py -= overy;
			eff.addEffect(new ReikaModelledBreakFX(world, x+rand.nextDouble(), y+rand.nextDouble(), z+rand.nextDouble(), -1+rand.nextDouble()*2, 2, -1+rand.nextDouble()*2, b, 0, world.getBlockMetadata(x, y, z), file, px, py, mod));
		}
		return true;
	}

	/** Renders break particles for custom-rendered TileEntities. Call this one from BlockDestroyEffects!
	 * Args: Texture Path, world, x, y, z, Block, EffectRenderer, Allowed Texture Regions <br>
	 * See addModelledBlockParticles(basedir, world, x, y, z, b, eff, allowedRegions) for explanation of the regions. */
	public static boolean addModelledBlockParticlesDirect(String texture, World world, int x, int y, int z, Block b, EffectRenderer eff, List<double[]> allowedRegions, Class mod) {
		for (int i = 0; i < 48; i++) {
			int k = rand.nextInt(allowedRegions.size());
			double[] p = allowedRegions.get(k);
			double px = p[0]+rand.nextDouble()*(p[2]-p[0]);
			double py = p[1]+rand.nextDouble()*(p[3]-p[1]);
			double overx = px+ReikaModelledBreakFX.pw-p[2];
			if (overx > 0)
				px -= overx;
			double overy = py+ReikaModelledBreakFX.pw-p[2];
			if (overy > 0)
				py -= overy;
			eff.addEffect(new ReikaModelledBreakFX(world, x+rand.nextDouble(), y+rand.nextDouble(), z+rand.nextDouble(), -1+rand.nextDouble()*2, 2, -1+rand.nextDouble()*2, b, 0, world.getBlockMetadata(x, y, z), texture, px, py, mod));
		}
		return true;
	}

	/** Renders break particles for custom-rendered TileEntities. Call this one from BlockHitEffects!
	 * Args: Texture Path, world, MovingObjectPosition, Block, EffectRenderer, Allowed Texture Regions. <br>
	 * See addModelledBlockParticles(basedir, world, x, y, z, b, eff, allowedRegions) for explanation of the regions. */
	public static boolean addModelledBlockParticlesDirect(String texture, World world, MovingObjectPosition mov, Block b, EffectRenderer eff, List<double[]> allowedRegions, Class mod) {
		if (mov == null)
			return false;
		int x = mov.blockX;
		int y = mov.blockY;
		int z = mov.blockZ;
		int j = 1+rand.nextInt(2);
		for (int i = 0; i < j; i++) {
			int k = rand.nextInt(allowedRegions.size());
			double[] p = allowedRegions.get(k);
			double px = p[0]+rand.nextDouble()*(p[2]-p[0]);
			double py = p[1]+rand.nextDouble()*(p[3]-p[1]);
			double overx = px+ReikaModelledBreakFX.pw-p[2];
			if (overx > 0)
				px -= overx;
			double overy = py+ReikaModelledBreakFX.pw-p[2];
			if (overy > 0)
				py -= overy;
			eff.addEffect(new ReikaModelledBreakFX(world, x+rand.nextDouble(), y+rand.nextDouble(), z+rand.nextDouble(), -1+rand.nextDouble()*2, 2, -1+rand.nextDouble()*2, b, 0, world.getBlockMetadata(x, y, z), texture, px, py, mod));
		}
		return true;
	}

	public static void spawnDropParticles(World world, int x, int y, int z, Block b, int meta) {
		for (int i = 0; i < 16; i++) {
			Minecraft.getMinecraft().effectRenderer.addEffect(new ReikaModelledBreakFX(world, x+rand.nextDouble(), y+rand.nextDouble(), z+rand.nextDouble(), -1+rand.nextDouble()*2, 2, -1+rand.nextDouble()*2, b, meta, 0));
		}
	}

	public static TesselatorVertexState getTessellatorState() {
		Entity e = Minecraft.getMinecraft().renderViewEntity;
		TesselatorVertexState st = Tessellator.instance.getVertexState((float)e.posX, (float)e.posY, (float)e.posZ);
		return st;
	}

}
