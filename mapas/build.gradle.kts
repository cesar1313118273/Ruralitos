// Paquete de recursos de Google Play con los mapas de Ecuador (≈ 490 MB). Se instala junto con la app
// (install-time), pero fuera del módulo base, que así queda pequeño. Los archivos se leen igual que antes
// con `context.assets`.
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("mapas")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
