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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackFileNotFoundException;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import Reika.DragonAPI.Exception.MisuseException;
import Reika.DragonAPI.IO.ReikaImageLoader;
import Reika.DragonAPI.IO.ReikaTextureBinder;
import Reika.DragonAPI.Instantiable.Data.PluralMap;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaDyeHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ReikaTextureHelper {

	/** Keys: Class, Path */
	private static final PluralMap textures = new PluralMap(2);

	private static final HashMap<ReikaDyeHelper, Integer> colorOverrides = new HashMap();

	public static final ReikaTextureBinder binder = new ReikaTextureBinder();

	private static final ResourceLocation font = new ResourceLocation("textures/font/ascii.png");
	private static final ResourceLocation particle = new ResourceLocation("textures/particle/particles.png");
	private static final ResourceLocation gui = new ResourceLocation("textures/gui/widgets.png");
	private static final ResourceLocation hud = new ResourceLocation("textures/gui/icons.png");

	private static boolean reload() {
		return Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.isKeyDown(Keyboard.KEY_T);
	}

	public static void bindTexture(Class root, String tex) {
		if (reload()) {
			textures.clear();
			colorOverrides.clear();
		}
		else {
			if (root == null) {
				throw new MisuseException("You cannot fetch a render texture with reference to a null class!");
			}
			String oldtex = tex;
			//String parent = root.getPackage().getName().replaceAll("\\.", "/")+"/";
			String s = root.getCanonicalName();
			String parent = s.substring(0, s.length()-root.getSimpleName().length()-1).replaceAll("\\.", "/")+"/";
			if (tex.startsWith("/"))
				tex = tex.substring(1);
			String respath = tex.startsWith(parent) ? tex : parent+tex;

			Integer gl = (Integer)textures.get(root, tex);
			if (gl == null) {
				boolean loaded = false;
				ArrayList<IResourcePack> li = getCurrentResourcePacks();
				for (int i = 0; i < li.size() && !loaded; i++) {
					IResourcePack res = li.get(i);
					gl = bindPackTexture(root, respath, res);
					if (gl != null) {
						textures.put(gl, root, tex);
						loaded = true;
						ReikaJavaLibrary.pConsole("DRAGONAPI: Texture Pack "+res.getPackName()+" contains an image for "+tex+".");
					}
				}
			}
			if (gl == null) {
				ReikaJavaLibrary.pConsole("DRAGONAPI: No texture packs contain an image for "+tex+". Loading default.");
				gl = bindClassReferencedTexture(root, oldtex);
				textures.put(gl, root, tex);
			}
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, gl.intValue());
		}
	}

	/** To disallow resource packs to change it */
	public static void bindFinalTexture(Class root, String tex) {
		Integer gl = (Integer)textures.get(root, tex);
		if (gl == null) {
			gl = bindClassReferencedTexture(root, tex);
			textures.put(gl, root, tex);
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, gl.intValue());
	}

	private static Integer bindClassReferencedTexture(Class root, String tex) {
		BufferedImage img = ReikaImageLoader.readImage(root, tex);
		if (img == null) {
			ReikaJavaLibrary.pConsole("No image found for "+tex+"!");
			return new Integer(binder.allocateAndSetupTexture(ReikaImageLoader.getMissingTex()));
		}
		else {
			return new Integer(binder.allocateAndSetupTexture(img));
		}
	}

	public static void bindRawTexture(Class root, String tex) {
		Integer gl = (Integer)textures.get(root, tex);
		if (gl == null) {
			BufferedImage img = ReikaImageLoader.readHardPathImage(tex);
			if (img == null) {
				ReikaJavaLibrary.pConsole("No image found for "+tex+"!");
				gl = new Integer(binder.allocateAndSetupTexture(ReikaImageLoader.getMissingTex()));
				textures.put(gl, root, tex);
			}
			else {
				gl = new Integer(binder.allocateAndSetupTexture(img));
				textures.put(gl, root, tex);
			}
		}
		if (gl != null)
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, gl.intValue());
	}

	private static Integer bindPackTexture(Class root, String tex, IResourcePack res) {
		BufferedImage img = ReikaImageLoader.getImageFromResourcePack(tex, res);
		return img != null ? new Integer(binder.allocateAndSetupTexture(img)) : null;
	}

	public static void bindTerrainTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
	}

	public static void bindFontTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(font);
	}

	public static void bindItemTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);
	}

	public static void bindGuiTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(gui);
	}

	public static void bindHUDTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(hud);
	}

	public static int getIconHeight() {
		return 16;
	}

	public static IIcon getMissingIcon() {
		TextureMap tex = (TextureMap)Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture);
		return tex.getAtlasSprite("missingno");
	}

	/** Overrides the standard ResourceLocation system. Unfortunately not yet functional. *//*
	public static void forceArmorTexturePath(String tex) {
		ReikaJavaLibrary.pConsole("DRAGONAPI: Disabling ResourceLocation on armor texture "+tex);
		ForcedResource f = new ForcedResource(tex);
		Map map = getArmorTextureMappings();
		map.put(tex, f);
	}

	private static Map getArmorTextureMappings() {
		try {
			Class c = RenderBiped.class;
			Field f = ReikaObfuscationHelper.getField("field_110859_k");
			f.setAccessible(true);
			return (Map)f.get(null);
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load the Armor Textures!");
		}
	}*/

	/** Returns a list of the selected resource packs, in the order they appear in the selection screen. */
	public static ArrayList<IResourcePack> getCurrentResourcePacks() {
		ArrayList<IResourcePack> packs = new ArrayList();
		List<ResourcePackRepository.Entry> li = Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries();
		for (int i = 0; i < li.size(); i++) {
			ResourcePackRepository.Entry e = li.get(i);
			packs.add(e.getResourcePack());
		}
		return packs;
	}

	public static boolean isDefaultResourcePack() {
		return Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries().isEmpty();
	}

	public static IResourcePack getDefaultResourcePack() {
		return Minecraft.getMinecraft().getResourcePackRepository().rprDefaultResourcePack;
	}

	public static InputStream getStreamFromTexturePack(String path, AbstractResourcePack pack) {
		try {
			Method m = ReikaObfuscationHelper.getMethod("getInputStreamByName");
			//m.setAccessible(true);
			Object o = m.invoke(pack, path);
			if (o == null)
				return null;
			InputStream in = (InputStream)o;
			//m.setAccessible(false);
			return in;
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof ResourcePackFileNotFoundException) {

			}
			else
				e.printStackTrace();
		}
		return null;
	}

	public static int getColorOverride(ReikaDyeHelper dye) {
		if (reload()) {
			colorOverrides.clear();
		}
		Integer color = colorOverrides.get(dye);
		if (color == null) {
			initializeColorOverrides();
			color = colorOverrides.get(dye);
		}
		return color != null ? color.intValue() : dye.getDefaultColor();
	}

	private static void initializeColorOverrides() {
		ArrayList<IResourcePack> li = getCurrentResourcePacks();
		boolean loaded = false;
		for (int k = 0; k < li.size(); k++) {
			AbstractResourcePack pack = (AbstractResourcePack)li.get(k);
			try {
				String path = "Reika/DragonAPI/dyecolor.txt";
				InputStream in = getStreamFromTexturePack(path, pack);
				if (in != null) {
					BufferedReader p = new BufferedReader(new InputStreamReader(in));
					for (int i = 0; i < 16; i++) {
						String line = p.readLine();
						String[] s = line.split(":");
						int c = Color.decode(s[1]).getRGB();
						Integer color = new Integer(c);
						ReikaDyeHelper dye = ReikaDyeHelper.dyes[i];
						colorOverrides.put(dye, color);
					}
					p.close();
					ReikaJavaLibrary.pConsole("DRAGONAPI: Found color override text file for texture pack "+pack.getPackName()+".");
					loaded = true;
				}
				else {
					ReikaJavaLibrary.pConsole("DRAGONAPI: No color override found for texture pack "+pack.getPackName()+".");
				}
			}
			catch (Exception e) {
				ReikaJavaLibrary.pConsole("DRAGONAPI: Error reading color override text file for texture pack "+pack.getPackName()+".");
				e.printStackTrace();
			}
		}
		if (!loaded) {
			ReikaJavaLibrary.pConsole("DRAGONAPI: Could not find color override text file in any resource packs. Using defaults.");
			for (int i = 0; i < 16; i++) {
				ReikaDyeHelper dye = ReikaDyeHelper.dyes[i];
				int c = dye.getDefaultColor();
				Integer color = new Integer(c);
				colorOverrides.put(dye, color);
			}
			return;
		}
	}

	public static void bindParticleTexture() {
		Minecraft.getMinecraft().renderEngine.bindTexture(particle);
	}

}
