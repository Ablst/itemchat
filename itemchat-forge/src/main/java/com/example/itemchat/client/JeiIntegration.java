package com.example.itemchat.client;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 的软依赖桥接层。
 *
 * <p>只有 JEI 已安装、并且插件的 {@code onRuntimeAvailable} 被调用过之后，
 * {@link #runtime} 才不为 null。所有对外方法都先判空，因此即使 JEI 不在也不会 NPE。</p>
 *
 * <p>这里用到的是 JEI 15.x（MC 1.20.1 线）的公开 API，签名没有变动：
 * {@code IIngredientListOverlay#getIngredientUnderMouse()}、
 * {@code IRecipesGui#getIngredientUnderMouse(IIngredientType)}、
 * {@code ITypedIngredient#getItemStack()}。</p>
 */
public final class JeiIntegration {

    private static IJeiRuntime runtime;

    private JeiIntegration() {
    }

    static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static void clearRuntime() {
        runtime = null;
    }

    /** 是否已拿到 JEI 运行时（没装 JEI、或 JEI 尚未初始化时为 false）。 */
    public static boolean isAvailable() {
        return runtime != null;
    }

    /**
     * 取当前鼠标悬停的那个物品，作为"要分享的物品"。
     *
     * <p>依次尝试三个来源，任一命中即返回：</p>
     * <ol>
     *   <li>物品管理器列表（容器界面右侧的浮层，以及配方界面右侧那一列）</li>
     *   <li>配方界面（按 R/U 打开）里的原料 / 产物</li>
     *   <li>书签栏</li>
     * </ol>
     *
     * <p>三者基本互斥（鼠标只会停在一处），所以顺序不影响正确性，
     * 只是把最常见的"物品列表"放在最前面。</p>
     */
    public static ItemStack getItemStackUnderMouse() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }

        ItemStack fromList = runtime.getIngredientListOverlay().getIngredientUnderMouse()
                .flatMap(ITypedIngredient::getItemStack)
                .orElse(ItemStack.EMPTY);
        if (!fromList.isEmpty()) {
            return fromList;
        }

        ItemStack fromRecipe = runtime.getRecipesGui()
                .getIngredientUnderMouse(VanillaTypes.ITEM_STACK)
                .orElse(ItemStack.EMPTY);
        if (!fromRecipe.isEmpty()) {
            return fromRecipe;
        }

        return runtime.getBookmarkOverlay().getIngredientUnderMouse()
                .flatMap(ITypedIngredient::getItemStack)
                .orElse(ItemStack.EMPTY);
    }

    /** 打开某物品的配方界面，等价于在 JEI 里对着它按 R。 */
    public static void showRecipes(ItemStack stack) {
        if (runtime == null || stack.isEmpty()) {
            return;
        }
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        runtime.getRecipesGui().show(focus);
    }
}
