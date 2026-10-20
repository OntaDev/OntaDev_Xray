package com.ontadev.xray;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.plugin.OntaDev_Template;

public final class OntaDevXrayPlugin extends OntaDev_Template {
    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        log.info("OntaDev_Xray enabled");
    }
}
