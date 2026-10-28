package com.example.itemchat.client;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 的软依赖桥接层。
 *
 * <p>本类直接引用了 JEI 的类，因此只有在确认 JEI 已加载之后才允许调用它的方法，
 * 否则会抛 {@code NoClassDefFoundError}。</p>
 *
 * <p>这里用到的是 JEI 19.x（MC 1.21.1 线）的公开 API，与 1.20.1 线的 JEI 15.x 签名一致：
 * {@code IIngredientListOverlay#getIngredientUnderMouse()}、
 * {@code IRecipesGui#getIngredientUnderMouse(IIngredientType)}、
 * {@code ITypedIngredient#getItemStack()}、{@code IJeiRuntime#getBookmarkOverlay()}。</p>
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
     *
     * <p>没装 JEI、或 JEI 尚未初始化（{@code runtime} 还是 null）时返回
     * {@link ItemStack#EMPTY}，调用方据此继续往下判断 FTB 任务和容器槽位。</p>
     */
    public static ItemStack getItemStackUnderMouse() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }

        // JEI 19 的 getIngredientUnderMouse() 返回 Optional<ITypedIngredient<?>>，
        // 这里用显式 lambda 而不是方法引用：通配符泛型 + 方法引用容易在类型推断上踩坑。
        ItemStack fromList = runtime.getIngredientListOverlay().getIngredientUnderMouse()
                .flatMap(typed -> typed.getItemStack())
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
                .flatMap(typed -> typed.getItemStack())
                .orElse(ItemStack.EMPTY);
    }

    /** 在 JEI 中打开该物品的配方界面（等价于在 JEI 里按 R）。 */
    public static void showRecipes(ItemStack stack) {
        if (runtime == null || stack.isEmpty()) {
            return;
        }
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        runtime.getRecipesGui().show(focus);
    }
}
