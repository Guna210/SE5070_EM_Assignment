package com.example.boardnotes.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.HashSet;
import java.util.Set;

public class NetworkMonitor extends LiveData<Boolean> {
    private static volatile NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final Set<Network> availableNetworks = new HashSet<>();
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            synchronized (availableNetworks) {
                availableNetworks.add(network);
                postValue(!availableNetworks.isEmpty());
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            synchronized (availableNetworks) {
                availableNetworks.remove(network);
                postValue(!availableNetworks.isEmpty());
            }
        }
    };

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) {
                    instance = new NetworkMonitor(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // Called by LiveData when the first observer arrives.
    @Override
    protected void onActive() {
        // Seed with the state right now - callbacks only report CHANGES.
        setValue(hasInternetNow());

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        connectivityManager.registerNetworkCallback(request, callback);
    }

    // Called by LiveData when the last observer goes away.
    @Override
    protected void onInactive() {
        connectivityManager.unregisterNetworkCallback(callback);
        synchronized (availableNetworks) {
            availableNetworks.clear();
        }
    }

    private boolean hasInternetNow() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
