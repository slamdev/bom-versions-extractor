package com.github.slamdev.bomversionsextractor;

import org.json.JSONArray;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@CommandLine.Command(mixinStandardHelpOptions = true, description = "extract versions")
public class Application implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"}, required = false, description = "store results in a file")
    private Optional<Path> outFile;

    @CommandLine.Option(names = {"-r", "--repo"}, arity = "1..*", required = true, description = "maven repos to use; repeatable")
    private List<String> repos;

    @CommandLine.Option(names = {"-b", "--bom"}, arity = "1..*", required = true, description = "bom to explore; repeatable")
    private List<String> boms;

    @CommandLine.Option(names = {"-c", "--cache-dir"}, required = false, description = "dir to stores maven caches")
    private Optional<Path> cacheDir;

    public static void main(String[] args) {
        int exitCode = new CommandLine(Application.class).execute(args);
        System.exit(exitCode);
    }

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void run() {
        try {
            Extractor e = new Extractor(repos, boms, cacheDir.orElse(Files.createTempDirectory("bom-versions-extractor")));
            List<ArtifactInfo> artifacts = e.run();
            JSONArray json = new JSONArray(artifacts);
            if (outFile.isPresent()) {
                Files.writeString(outFile.get(), json.toString());
            } else {
                System.out.println(json.toString(2));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
