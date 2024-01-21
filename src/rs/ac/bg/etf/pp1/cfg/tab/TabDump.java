package rs.ac.bg.etf.pp1.cfg.tab;

import java.io.IOException;
import java.nio.file.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Scope;

public class TabDump {

	public static void saveTab(String fileName) {
		try {
			
			TabVisitor stv = new TabVisitor();
			for (Scope s = Tab.currentScope; s != null; s = s.getOuter()) {
				s.accept(stv);
			}
            // Convert the string to bytes
			String content = stv.getOutput();
            byte[] contentBytes = content.getBytes();

            // Write the bytes to the file
            Path filePath = Paths.get(fileName);
            Files.write(filePath, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
