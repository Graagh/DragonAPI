/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2016
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Libraries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import Reika.DragonAPI.Instantiable.HybridTank;
import Reika.DragonAPI.Instantiable.Data.Maps.MultiMap;

public class ReikaFluidHelper {

	private static final MultiMap<Fluid, FluidContainer> containers = new MultiMap();

	private static final HashMap<String, String> nameSwaps = new HashMap();
	private static final HashMap<String, Integer> fluidColorMap = new HashMap();

	static {
		fluidColorMap.put("water", 0x0065FF);
		fluidColorMap.put("lava", 0xFF3300);
		fluidColorMap.put("ender", 0x006470);
		fluidColorMap.put("glowstone", 0xFFE45E);
		fluidColorMap.put("redstone", 0xBC0000);
		fluidColorMap.put("cryotheum", 0x42DFFF);
		fluidColorMap.put("steam", 0xd0d0d0);
		fluidColorMap.put("xp", 0x84FF00);
		fluidColorMap.put("for.honey", 0xFFA300);
		fluidColorMap.put("ic2distilledwater", 0x4F65E2);
		fluidColorMap.put("oil", 0x101010);
		fluidColorMap.put("fuel", 0xC4A300);
		fluidColorMap.put("creosote", 0x963700);
		fluidColorMap.put("biomass", 0x35A536);
	}

	private static boolean init = false;

	public static void initEarlyRegistrations() {
		if (!init) {
			FluidContainerData[] dat = FluidContainerRegistry.getRegisteredFluidContainerData();
			for (int i = 0; i < dat.length; i++) {
				FluidContainerData fcd = dat[i];
				if (fcd.fluid != null && fcd.filledContainer != null) {
					mapContainerToFluid(fcd.fluid.getFluid(), fcd.emptyContainer, fcd.filledContainer);
				}
			}

			init = true;
		}
	}

	public static void mapContainerToFluid(Fluid f, ItemStack empty, ItemStack filled) {
		containers.addValue(f, new FluidContainer(filled, empty));
	}

	public static ArrayList<ItemStack> getAllContainersFor(Fluid f) {
		ArrayList<ItemStack> c = new ArrayList();
		for (FluidContainer fc : containers.get(f)) {
			if (fc.filled != null)
				c.add(fc.filled);
		}
		return c;
	}

	public static ArrayList<ItemStack> getAllEmptyContainers() {
		ArrayList<ItemStack> c = new ArrayList();
		for (FluidContainer fc : containers.allValues(false)) {
			if (fc.empty != null)
				c.add(fc.empty);
		}
		return c;
	}

	public static boolean isInfinite(Fluid f) {
		return f == FluidRegistry.WATER;
	}

	private static class FluidContainer {

		private final ItemStack filled;
		private final ItemStack empty;

		private FluidContainer(ItemStack fill, ItemStack emp) {
			filled = fill;
			empty = emp;
		}

	}

	public static void sortFluids(ArrayList<FluidStack> li) {
		Collections.sort(li, fluidStackComparator);
	}

	public static final Comparator<FluidStack> fluidStackComparator = new FluidStackComparator();

	private static class FluidStackComparator implements Comparator<FluidStack> {

		@Override
		public int compare(FluidStack o1, FluidStack o2) {
			if (o1.getFluidID() == o2.getFluidID()) {
				return o1.amount-o2.amount;
			}
			else {
				return o1.getFluidID()-o2.getFluidID();
			}
		}

	}

	public static String fluidToString(Fluid f) {
		return f.getUnlocalizedName()+"["+f+"]("+f.getID()+")";
	}

	public static String fluidStackToString(FluidStack f) {
		return f.amount+"x"+f.getUnlocalizedName()+"["+f.getFluid()+"]("+f.getFluidID()+")"+"{"+f.tag+"}";
	}

	public static void registerNameSwap(String old, String next) {
		nameSwaps.put(old, next);
	}

	public static String getFluidNameSwap(String oldName) {
		return nameSwaps.get(oldName);
	}

	public static String getOldNameIfApplicable(String fluidName) {
		if (fluidName == null)
			return fluidName;
		String repl = nameSwaps.get(fluidName);
		return repl != null ? repl : fluidName;
	}

	public static boolean isFluidNullOrMatch(Fluid f, HybridTank tank) {
		return f == null || f == tank.getActualFluid();
	}

	public static boolean isFluidDrainableFromTank(Fluid f, HybridTank tank) {
		return !tank.isEmpty() && isFluidNullOrMatch(f, tank);
	}

	public static boolean isFlammable(Fluid f) {
		String s = f.getName().toLowerCase(Locale.ENGLISH);
		if (s.contains("fuel"))
			return true;
		if (s.contains("ethanol"))
			return true;
		if (s.contains("oil"))
			return true;
		if (s.equals("creosote"))
			return true;
		if (s.contains("gas"))
			return true;
		if (s.endsWith("ane") || s.endsWith("ene") || s.endsWith("yne")) //Hydrocarbons
			return true;
		if (s.endsWith("ol") || s.endsWith("al") || s.endsWith("one")) //Other organics
			return true;
		return false;
	}

	public static int getFluidColor(Fluid f) {
		int c = f.getColor();
		if (c == 0xffffff && fluidColorMap.containsKey(f.getName())) {
			c = fluidColorMap.get(f.getName());
		}
		return c;
	}

}
