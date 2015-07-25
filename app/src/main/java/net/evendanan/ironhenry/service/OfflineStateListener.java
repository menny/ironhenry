package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.OfflineState;

public interface OfflineStateListener {
    void onOfflineStateChanged(@NonNull Iterable<OfflineState> offlineStates);
}
