package eu.icred.external.plugin.biis.xml;

import eu.icred.external.plugin.biis.xml.read.Reader;
import eu.icred.plugin.IPlugin;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.output.IExportWorker;

public class Plugin implements IPlugin {

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#isModelVersionSupported(java.lang.String)
     */
    public boolean isModelVersionSupported(String version) {
        return version.startsWith("1-0.6.");
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginId()
     */
    @Override
    public String getPluginId() {
        return "biis.xml";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginVersion()
     */
    @Override
    public String getPluginVersion() {
        return "0.6";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginName()
     */
    @Override
    public String getPluginName() {
        return "BIIS-XML-Plugin";
    }
    
    @Override
    public IImportWorker getImportPlugin() {
        return new Reader();
    }

    @Override
    public IExportWorker getExportPlugin() {
        return null;
    }

}
