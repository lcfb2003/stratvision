package br.edu.utfpr.stratvision.persistence;

import br.edu.utfpr.stratvision.patlan.Pattern;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * @author Luis Carlos F. Bueno - 24/11/2021
 */
public class PatternDAO {

    public static Pattern loadPattern(String path) throws IOException {
        Path filePath = Paths.get(path);
        List<String> lines = Files.readAllLines(filePath);
        StringBuilder sb = new StringBuilder();
        sb.insert(0, lines.toArray());
       
        Pattern padrao = new Pattern();
        padrao.setSource(sb.toString());

        return padrao;
    }
    
    public static void savePattern(Pattern pattern, String path) throws IOException {
        try (PrintWriter print = new PrintWriter(path)) {
            print.print(pattern.getSource());
        }
    }
}
