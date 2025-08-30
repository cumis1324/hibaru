package com.theflexproject.thunder.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomIndex {
    private final List<String> loadBalancerDomains = Arrays.asList(
            "lb1.nfgplusmirror.workers.dev",
            "lb2.nfgplusmirror.workers.dev",
            "lb3.nfgplusmirror.workers.dev",
            "drive4.nfgplusmirror.workers.dev"
    );
    private final Random random = new Random();

    // 2. Pindahkan logika pemilihan ke dalam method ini.
    // Sekarang, setiap kali method ini dipanggil, ia akan memilih domain baru.
    public String getSelectedDomain() {
        // Ambil domain dari list menggunakan indeks acak yang baru dibuat
        return loadBalancerDomains.get(random.nextInt(loadBalancerDomains.size()));
    }
}
