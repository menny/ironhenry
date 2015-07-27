package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.Categories;

public interface CategoriesCallback {
    void onCategoriesAvailable(@NonNull Categories categories);
}
