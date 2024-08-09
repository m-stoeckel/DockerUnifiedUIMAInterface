import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.dkpro.core.api.resources.CompressionMethod;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUISwarmDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUICollectionReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReaderLazy;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.DockerUnifiedUIMAInterface.segmentation.DUUISegmentationStrategyByAnnotationFast;

import java.io.File;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class BigCorpus {

    @Test
    public void EUBooks() throws Exception {

        int iWorker = 1;

//        String sInputPath = "/mnt/NegLab/corpora/BigCorpus/EUBookshop";
//        String sOutputPath = "/tmp/EUBookshopSpaCy";
        String sInputPath = "/tmp/EUBook/input";
        String sOutputPath = "/tmp/EUBook/output";
////        String sOutputPath = "/tmp/wiki/";
        String sSuffix = "xmi.bz2";

        DUUICollectionReader pReader = new DUUIFileReaderLazy(sInputPath, sSuffix, sOutputPath, ".xmi.bz2", 1);

        // Asynchroner reader für die Input-Dateien
        DUUIAsynchronousProcessor pProcessor = new DUUIAsynchronousProcessor(pReader);
        new File(sOutputPath).mkdir();

        DUUILuaContext ctx = new DUUILuaContext().withJsonLibrary();

        // Instanziierung des Composers, mit einigen Parametern
        DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)     // wir überspringen die Verifikation aller Componenten =)
                .withLuaContext(ctx)            // wir setzen den definierten Kontext
                .withWorkers(iWorker);         // wir geben dem Composer eine Anzahl an Threads mit.

        DUUIDockerDriver docker_driver = new DUUIDockerDriver();
        DUUISwarmDriver swarm_driver = new DUUISwarmDriver();
        DUUIUIMADriver uima_driver = new DUUIUIMADriver()
                .withDebug(true);

        // Hinzufügen der einzelnen Driver zum Composer
        composer.addDriver(docker_driver, uima_driver, swarm_driver);  // remote_driver und swarm_driver scheint nicht benötigt zu werden.

        DUUISegmentationStrategyByAnnotationFast segmentationStrategy = new DUUISegmentationStrategyByAnnotationFast();
        segmentationStrategy.withSegmentationClass(Sentence.class);
        segmentationStrategy.withLength(500000);

        DUUIPipelineComponent componentLang = new DUUISwarmDriver
                //DUUIPipelineComponent componentLang = new DUUIDockerDriver
                .Component("docker.texttechnologylab.org/languagedetection:0.5")
                .withScale(iWorker)
                .build();
        composer.add(componentLang);

        composer.add(new DUUIDockerDriver.Component("docker.texttechnologylab.org/duui-spacy-de_core_news_lg:0.4.4")
                .withParameter("use_existing_tokens", String.valueOf(true))
                .withParameter("use_existing_sentences", String.valueOf(true))
                .withScale(iWorker)
                .withSegmentationStrategy(segmentationStrategy)
                .build());

        composer.add(new DUUIUIMADriver.Component(createEngineDescription(XmiWriter.class,
                XmiWriter.PARAM_TARGET_LOCATION, sOutputPath,
                XmiWriter.PARAM_PRETTY_PRINT, true,
                XmiWriter.PARAM_OVERWRITE, true,
                XmiWriter.PARAM_VERSION, "1.1",
                XmiWriter.PARAM_COMPRESSION, CompressionMethod.BZIP2
        )).withScale(iWorker).build());

        composer.run(pProcessor, "eubook");
    }

}
