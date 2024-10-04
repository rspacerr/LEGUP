package edu.rpi.legup.model;

import edu.rpi.legup.model.gameboard.PuzzleElement;
import edu.rpi.legup.model.tree.TreeNode;
import edu.rpi.legup.model.tree.TreeTransition;
import edu.rpi.legup.save.ExportFileException;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class PuzzleExporter {
    private static final Logger LOGGER = LogManager.getLogger(PuzzleExporter.class.getName());

    protected Puzzle puzzle;

    /**
     * PuzzleExporter Constructor exports the puzzle object to a file
     *
     * @param puzzle puzzle that is to be exported
     */
    public PuzzleExporter(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Takes the puzzle state and the current date/time to obfuscate
     * the solved state with an onto function to prevent cheating
     *
     * @param solved the solved state of the board, true if solved
     * @param date the current date and time, passed during export
     * @return hash value of time and solved state
     */
    public static int obfHash(boolean solved, String date) {
        LocalDateTime dateTime = LocalDateTime.parse(date, DATE_FORMAT);
        String obf = solved + ":" + dateTime.toEpochSecond(ZoneOffset.UTC) + ";";
        LOGGER.debug("Hashed Val: '{}'", obf);
        return obf.hashCode();
    }

    /**
     * Deobfuscates the solved state of the board from hash value using
     * the time provided in the puzzle xml-style export
     *
     * @param hash the hash value saved to the export
     * @param date the date/time value saved to the export
     * @return boolean value of the solved state of the board
     */
    public static boolean inverseHash(int hash, String date) {
        LocalDateTime dateTime = LocalDateTime.parse(date, DATE_FORMAT);
        long timestamp = dateTime.toEpochSecond(ZoneOffset.UTC);

        if ((true+":"+timestamp+";").hashCode() == hash) {
            return true;
        } else if ((false+":"+timestamp+";").hashCode() == hash) {
            return false;
        } else {
            throw new IllegalArgumentException("Hash does not inverse hash");
        }
    }

    /**
     * Exports the puzzle to an xml formatted file
     *
     * @param fileName name of file to be exported
     * @throws ExportFileException if puzzle can not be exported
     */
    public void exportPuzzle(String fileName) throws ExportFileException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document newDocument = docBuilder.newDocument();

            org.w3c.dom.Element legupElement = newDocument.createElement("Legup");
            legupElement.setAttribute("version", "3.0.0");
            newDocument.appendChild(legupElement);

            org.w3c.dom.Element timeSavedElement = newDocument.createElement("saved");
            legupElement.appendChild(timeSavedElement);

            org.w3c.dom.Element puzzleElement = newDocument.createElement("puzzle");
            puzzleElement.setAttribute("name", puzzle.getName());

            // adds puzzle id as file name if untagged
            if (this.puzzle.tag.isEmpty()) {
                // format string to only be export file name (without extension
                String idStr = fileName.substring(fileName.lastIndexOf("/") + 1);
                idStr = idStr.contains(".") ? idStr.substring(0, idStr.lastIndexOf('.')) : idStr;
                puzzleElement.setAttribute("id", idStr);
            } else {
                puzzleElement.setAttribute("id", this.puzzle.tag);
            }

            legupElement.appendChild(puzzleElement);

            puzzleElement.appendChild(createBoardElement(newDocument));
            if (puzzle.getTree() != null
                    && !puzzle.getTree().getRootNode().getChildren().isEmpty()) {
                puzzleElement.appendChild(createProofElement(newDocument));
            }

            org.w3c.dom.Element statusElement = newDocument.createElement("solved");
            LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("America/New_York"));
            String time = dateTime.format(DATE_FORMAT);
            statusElement.setAttribute("lastSaved", time);
            int hashedState = obfHash(puzzle.isPuzzleComplete(), time);
            statusElement.setAttribute("isSolved", hashedState+"");
            legupElement.appendChild(statusElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(newDocument);
            StreamResult result = new StreamResult(new File(fileName));

            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new ExportFileException("Puzzle Exporter: parser configuration exception");
        } catch (Exception e) {
            throw e;
            // throw new ExportFileException(e.getMessage());
        }
    }

    protected abstract Element createBoardElement(Document newDocument);

    protected Element createProofElement(Document newDocument) {
        org.w3c.dom.Element proofElement = newDocument.createElement("proof");
        org.w3c.dom.Element treeElement = createTreeElement(newDocument);
        proofElement.appendChild(treeElement);
        return proofElement;
    }

    protected Element createTreeElement(Document newDocument) {
        org.w3c.dom.Element treeElement = newDocument.createElement("tree");

        Set<TreeNode> visited = new HashSet<>();
        List<TreeNode> nodes = new ArrayList<>();
        nodes.add(puzzle.getTree().getRootNode());
        while (!nodes.isEmpty()) {
            TreeNode treeNode = nodes.get(nodes.size() - 1);
            nodes.remove(treeNode);
            if (!visited.contains(treeNode)) {
                visited.add(treeNode);

                org.w3c.dom.Element nodeElement = newDocument.createElement("node");
                nodeElement.setAttribute("id", String.valueOf(treeNode.hashCode()));
                if (treeNode.isRoot()) {
                    nodeElement.setAttribute("root", "true");
                }

                for (TreeTransition transition : treeNode.getChildren()) {
                    org.w3c.dom.Element transElement = newDocument.createElement("transition");
                    transElement.setAttribute("id", String.valueOf(transition.hashCode()));

                    TreeNode child = transition.getChildNode();
                    if (child != null) {
                        transElement.setAttribute("child", String.valueOf(child.hashCode()));
                        nodes.add(child);
                    }

                    if (transition.isJustified()) {
                        transElement.setAttribute("rule", transition.getRule().getRuleName());
                        transElement.setAttribute("rule_id", transition.getRule().getRuleID());
                    }

                    for (PuzzleElement data : transition.getBoard().getModifiedData()) {
                        transElement.appendChild(puzzle.getFactory().exportCell(newDocument, data));
                    }
                    nodeElement.appendChild(transElement);
                }
                treeElement.appendChild(nodeElement);
            }
        }
        return treeElement;
    }
}
