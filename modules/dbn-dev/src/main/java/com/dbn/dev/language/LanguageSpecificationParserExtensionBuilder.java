/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.dev.language;

import com.dbn.common.util.Lists;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.path.LanguageNodeBase;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.dev.language.LanguageSpecificationXmlUtil.fileToDocument;
import static com.dbn.dev.language.LanguageSpecificationXmlUtil.outputPrettyString;
import static com.dbn.language.common.element.util.ElementTypeAttribute.OPTIONAL_WRAPPING;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SYNTHETIC;

public class LanguageSpecificationParserExtensionBuilder implements LanguageSpecificationArtifactBuilder {
    private static final String EXT_DTD_PATH = "../../../common/definition/language-parser-elements-ext.dtd";
    private static final String BUILDER_VERSION = "2.1.0";
    private static final String GENERATED_COMMENT =
            "Generated with LanguageSpecificationParserExtensionBuilder. Do not edit manually.";
    private static final String ATTR_TOKEN_TYPE_IDS = "tt";
    private static final String ATTR_PARSE_CANDIDATE_IDS = "pc";
    private static final String ATTR_COMPLETION_CANDIDATE_IDS = "cc";
    private static final String ATTR_NODE_ID = "id";
    private static final String ATTR_NODE_REF = "ref";
    private static final int DEFAULT_MAX_DEPTH = 7;
    private static final int DEFAULT_MAX_COMPLETION_CANDIDATES = 300;
    private static final int DEFAULT_MAX_EXTENSION_CANDIDATES = 10;

    private final LanguageSpecificationBuilderInput input;
    private final LanguageSpecificationNextLeafResolver nextLeafResolver =
            new LanguageSpecificationNextLeafResolver();
    private final Map<String, ExtensionNode> extensionNodes = new LinkedHashMap<>();
    private int maxDepth = DEFAULT_MAX_DEPTH;
    private int maxCompletionCandidates = DEFAULT_MAX_COMPLETION_CANDIDATES;
    private int maxExtensionCandidates = DEFAULT_MAX_EXTENSION_CANDIDATES;

    public LanguageSpecificationParserExtensionBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @Override
    public void build() throws Exception {
        loadConfiguration();
        new LanguageSpecificationParserBundleLoader(input).load(
                (bundle, builder) -> buildExtension(bundle, builder),
                false);
    }

    private void loadConfiguration() throws Exception {
        Document definitionDocument = fileToDocument(input.getParserElementsFile());
        if (definitionDocument == null) {
            throw new IllegalStateException(
                    "Could not load parser elements definition " + input.getParserElementsFile());
        }

        Element definitionRoot = definitionDocument.getRootElement();
        maxDepth = integerAttribute(definitionRoot, "max-depth", DEFAULT_MAX_DEPTH);
        maxCompletionCandidates = integerAttribute(definitionRoot, "max-completion-candidates", DEFAULT_MAX_COMPLETION_CANDIDATES);
        maxExtensionCandidates = integerAttribute(definitionRoot, "max-extension-candidates", DEFAULT_MAX_EXTENSION_CANDIDATES);
    }

    private void buildExtension(ElementTypeBundle bundle, ElementTypeBundle.Builder builder) {
        List<OneOfElementType> oneOfElements = builder
                .getElementTypes()
                .values()
                .stream()
                .filter(e -> e instanceof OneOfElementType)
                .map(e -> (OneOfElementType) e)
                .sorted()
                .toList();
        for (OneOfElementType oneOfElement : oneOfElements) {
            buildExtension(oneOfElement);
        }

        writeExtensionFile(bundle);
    }

    private void buildExtension(OneOfElementType oneOfElement) {
        System.out.print(" " + oneOfElement.getId());

        ExtensionNode extensionNode = new ExtensionNode();
        LanguageNodeBase rootNode = new LanguageNodeBase(oneOfElement, null);

        for (ElementTypeRef element : oneOfElement.children) {
            LanguageNodeBase childNode = new LanguageNodeBase(element.elementType, rootNode);
            for (LanguageNodeBase leafNode : nextLeafResolver.getFirstPossibleLeafs(childNode)) {
                LeafElementType leaf = (LeafElementType) leafNode.element;
                Node childExtensionNode = extensionNode.getOrCreateChild(leaf.tokenType);
                addChildNode(childExtensionNode, element.elementType, leafNode);
            }
        }

        if (oneOfElement.children.length <= maxExtensionCandidates &&
                !extensionNode.isAmbiguous()) return;

        extensionNodes.put(oneOfElement.getId(), extensionNode);

        extensionNode.aggregateChildren(1 >= maxDepth);
        for (Node node : extensionNode.childNodes) {
            expandNode(node, 1, maxDepth);
        }

    }

    private void expandNode(Node node, int depth, int maxDepth) {
        if (depth >= maxDepth) return;
        if (!node.isAmbiguous()) return;
        if (!node.isExpandable()) return;
        if (node.getCompletionCandidates().size() > maxCompletionCandidates) return;

        for (Map.Entry<ElementTypeBase, Set<LanguageNodeBase>> entry : node.nextLeafs.entrySet()) {
            ElementTypeBase parseCandidate = entry.getKey();
            for (LanguageNodeBase nextLeafNode : entry.getValue()) {
                LeafElementType nextLeaf = (LeafElementType) nextLeafNode.element;
                Node childNode = node.getOrCreateChild(nextLeaf.tokenType);
                addChildNode(childNode, parseCandidate, nextLeafNode);
            }
        }

        node.aggregateChildren(depth + 1 >= maxDepth);
        for (Node childNode : node.childNodes) {
            expandNode(childNode, depth + 1, maxDepth);
        }
    }

    private void addChildNode(
            Node node,
            ElementTypeBase parseCandidate,
            LanguageNodeBase leafNode) {
        node.parseCandidates.add(parseCandidate);
        if (node.recursivePath) return;

        LanguageNodeBase pathNode = leafNode;
        while (pathNode != null) {
            if (pathNode.isRecursive()) {
                node.recursivePath = true;
                node.nextLeafs.clear();
                node.completionCandidates = null;
                return;
            }
            pathNode = pathNode.getParent();
        }

        Set<LanguageNodeBase> nextLeafs = nextLeafResolver.getNextPossibleLeafs(leafNode);
        node.nextLeafs
                .computeIfAbsent(parseCandidate, candidate -> new LinkedHashSet<>())
                .addAll(nextLeafs);
        node.completionCandidates = null;
    }

    private void writeExtensionFile(ElementTypeBundle bundle) {
        try {
            Document definitionDocument = fileToDocument(input.getParserElementsFile());
            if (definitionDocument == null) {
                throw new IllegalStateException(
                        "Could not load parser elements definition " + input.getParserElementsFile());
            }

            Element extensionRoot = new Element("parser-element-extensions");
            extensionRoot.setAttribute("language", bundle.getLanguageDialect().getID());
            extensionRoot.setAttribute("source", input.getParserElementsFile().getName());
            extensionRoot.setAttribute("builder-version", BUILDER_VERSION);

            NodeIdSequence nodeIds = new NodeIdSequence();
            for (Map.Entry<String, ExtensionNode> entry : extensionNodes.entrySet()) {
                addOneOfExtension(extensionRoot, entry.getKey(), entry.getValue(), nodeIds);
            }

            Document extensionDocument = new Document(extensionRoot);
            extensionDocument.addContent(0, new DocType("parser-element-extensions", EXT_DTD_PATH));
            copyCopyright(definitionDocument, extensionDocument);
            addGeneratedComment(extensionDocument);

            File extensionFile = input.getParserElementsExtensionFile();
            Files.writeString(
                    extensionFile.toPath(),
                    outputPrettyString(extensionDocument),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write parser extension definition", e);
        }
    }

    private void addOneOfExtension(
            Element extensionRoot,
            String oneOfId,
            ExtensionNode extensionNode,
            NodeIdSequence nodeIds) {
        if (extensionNode.childNodes.isEmpty()) return;

        Element extensionElement = new Element("one-of-extension");
        extensionElement.setAttribute("id", oneOfId);
        extensionElement.setAttribute("depth", Integer.toString(getChildDepth(extensionNode.childNodes)));
        XmlWriteContext context = new XmlWriteContext(extensionNode.childNodes, nodeIds);
        for (Node childNode : extensionNode.childNodes) {
            extensionElement.addContent(toXmlElement(childNode, null, context));
        }
        extensionRoot.addContent(extensionElement);
    }

    private Element toXmlElement(
            Node node,
            Set<ElementTypeBase> inheritedCandidates,
            XmlWriteContext context) {
        if (node.tokenTypes.isEmpty()) {
            throw new IllegalStateException("Extension node must contain at least one token type");
        }

        SharedNode sharedNode = context.sharedNodes.get(node);
        if (sharedNode.referenced && sharedNode.id != null) {
            Element reference = new Element("node");
            reference.setAttribute(ATTR_NODE_REF, sharedNode.id);
            return reference;
        }

        Element element = new Element("node");
        boolean shared = sharedNode.referenced;
        if (shared) {
            sharedNode.id = context.nextNodeId();
            element.setAttribute(ATTR_NODE_ID, sharedNode.id);
        }
        element.setAttribute(ATTR_TOKEN_TYPE_IDS, Lists.toCsv(node.tokenTypes, TokenType::getId));
        if (shared || inheritedCandidates == null ||
                !Node.hasSameCandidateIds(node.parseCandidates, inheritedCandidates)) {
            setCandidateIds(element, ATTR_PARSE_CANDIDATE_IDS, node.parseCandidates);
        }
        Set<LeafElementType> completionCandidates = node.getCompletionCandidates();
        if (completionCandidates.size() <= maxCompletionCandidates) {
            setCandidateIds(element, ATTR_COMPLETION_CANDIDATE_IDS, completionCandidates);
        }

        if (node.isAmbiguous()) {
            for (Node childNode : node.childNodes) {
                element.addContent(toXmlElement(childNode, node.parseCandidates, context));
            }
        }
        return element;
    }

    private static void setCandidateIds(
            Element element,
            String attributeName,
            Set<? extends ElementTypeBase> candidates) {
        if (!candidates.isEmpty()) {
            element.setAttribute(attributeName, Lists.toCsv(candidates, ElementTypeBase::getId));
        }
    }

    private static int getChildDepth(List<Node> childNodes) {
        int depth = 0;
        for (Node childNode : childNodes) {
            int childDepth = childNode.isAmbiguous() ?
                    1 + getChildDepth(childNode.childNodes) :
                    1;
            depth = Math.max(depth, childDepth);
        }
        return depth;
    }

    private static void copyCopyright(Document sourceDocument, Document extensionDocument) {
        for (Object content : sourceDocument.getContent()) {
            if (content instanceof Comment comment && comment.getText().contains("Copyright")) {
                extensionDocument.addContent(0, comment.clone());
                return;
            }
        }
    }

    private static void addGeneratedComment(Document extensionDocument) {
        int rootIndex = extensionDocument.indexOf(extensionDocument.getRootElement());
        extensionDocument.addContent(rootIndex, new Comment(GENERATED_COMMENT));
    }

    private static class ExtensionNode {
        protected List<Node> childNodes = new ArrayList<>();

        protected Node getOrCreateChild(TokenType tokenType) {
            for (Node childNode : childNodes) {
                if (childNode.tokenTypes.contains(tokenType)) return childNode;
            }

            Node childNode = new Node(tokenType);
            childNodes.add(childNode);
            return childNode;
        }

        protected void aggregateChildren(boolean maxDepthReached) {
            List<Node> aggregatedChildNodes = new ArrayList<>();
            Map<Integer, List<Node>> aggregatedNodesByHash = new HashMap<>();
            for (Node sourceNode : childNodes) {
                boolean terminal = maxDepthReached ||
                        !sourceNode.isAmbiguous() ||
                        !sourceNode.isExpandable();
                int structureHash = sourceNode.getAggregationHash(terminal);
                List<Node> equivalentNodes = aggregatedNodesByHash.computeIfAbsent(
                        structureHash,
                        hash -> new ArrayList<>());
                Node aggregatedNode = Node.findEquivalentNode(equivalentNodes, sourceNode, terminal);
                if (aggregatedNode == null) {
                    aggregatedChildNodes.add(sourceNode);
                    equivalentNodes.add(sourceNode);
                } else {
                    aggregatedNode.tokenTypes.addAll(sourceNode.tokenTypes);
                    if (terminal) {
                        aggregatedNode.getCompletionCandidates().addAll(sourceNode.getCompletionCandidates());
                    }
                }
            }
            childNodes = aggregatedChildNodes;
        }

        protected boolean isAmbiguous() {
            for (Node childNode : childNodes) {
                if (childNode.isAmbiguous()) return true;
            }
            return false;
        }
    }

    private static final class Node extends ExtensionNode {
        private final Set<TokenType> tokenTypes = new LinkedHashSet<>();
        private final Set<ElementTypeBase> parseCandidates = new LinkedHashSet<>();
        private final Map<ElementTypeBase, Set<LanguageNodeBase>> nextLeafs = new LinkedHashMap<>();
        private Set<LeafElementType> completionCandidates;
        private boolean recursivePath;

        private Node(TokenType tokenType) {
            tokenTypes.add(tokenType);
        }

        @Override
        public String toString() {
            return tokenTypes.toString();
        }

        @Override
        protected boolean isAmbiguous() {
            return parseCandidates.size() > 1;
        }

        private Set<LeafElementType> getCompletionCandidates() {
            if (completionCandidates == null) {
                completionCandidates = nextLeafs.values().stream()
                        .flatMap(Set::stream)
                        .map(node -> (LeafElementType) node.element)
                        .filter(Node::isCompletionCandidate)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            return completionCandidates;
        }

        private int getStructureHash() {
            int hash = getCandidateIdsHash(parseCandidates);
            hash = 31 * hash + getCandidateIdsHash(getCompletionCandidates());

            int childHash = 0;
            for (Node childNode : childNodes) {
                childHash += 31 * childNode.tokenTypes.hashCode() + childNode.getStructureHash();
            }
            return 31 * hash + childHash;
        }

        private int getAggregationHash(boolean terminal) {
            int hash = terminal ? getCandidateIdsHash(parseCandidates) : getStructureHash();
            return 31 * hash + Boolean.hashCode(isExpandable());
        }

        private static Node findEquivalentNode(
                List<Node> nodes,
                Node sourceNode,
                boolean terminal) {
            for (Node node : nodes) {
                if (node.isExpandable() != sourceNode.isExpandable()) continue;
                boolean equivalent = terminal ?
                        hasSameCandidateIds(node.parseCandidates, sourceNode.parseCandidates) :
                        node.hasSameStructure(sourceNode);
                if (equivalent) return node;
            }
            return null;
        }

        private boolean hasSameStructure(Node node) {
            if (isExpandable() != node.isExpandable()) return false;
            if (!hasSameCandidateIds(parseCandidates, node.parseCandidates)) return false;
            if (!hasSameCandidateIds(getCompletionCandidates(), node.getCompletionCandidates())) return false;
            if (childNodes.size() != node.childNodes.size()) return false;

            for (Node firstChild : childNodes) {
                Node secondChild = findNode(node.childNodes, firstChild.tokenTypes);
                if (secondChild == null) return false;
                if (!firstChild.hasSameStructure(secondChild)) return false;
            }
            return true;
        }

        private boolean isExpandable() {
            if (recursivePath) return false;
            for (TokenType tokenType : tokenTypes) {
                if (tokenType.isOperator()) return false;
                if (tokenType.isCharacter()) return false;
            }
            return true;
        }

        private static Node findNode(List<Node> nodes, Set<TokenType> tokenTypes) {
            for (Node node : nodes) {
                if (node.tokenTypes.equals(tokenTypes)) return node;
            }
            return null;
        }

        private static boolean hasSameCandidateIds(
                Set<? extends ElementTypeBase> firstCandidates,
                Set<? extends ElementTypeBase> secondCandidates) {
            if (firstCandidates.size() != secondCandidates.size()) return false;

            var firstIterator = firstCandidates.iterator();
            var secondIterator = secondCandidates.iterator();
            while (firstIterator.hasNext()) {
                if (firstIterator.next() != secondIterator.next()) return false;
            }
            return true;
        }

        private static int getCandidateIdsHash(Set<? extends ElementTypeBase> candidates) {
            int hash = 1;
            for (ElementTypeBase candidate : candidates) {
                hash = 31 * hash + candidate.getId().hashCode();
            }
            return hash;
        }

        private static boolean isCompletionCandidate(LeafElementType leaf) {
            if (leaf.is(OPTIONAL_WRAPPING)) return false;
            if (leaf.is(SYNTHETIC)) return false;
            if (leaf.tokenType.isCharacter()) return false;
            if (leaf.tokenType.isOperator()) return false;
            return true;
        }
    }

    private final class XmlWriteContext {
        private final Map<Node, SharedNode> sharedNodes = new IdentityHashMap<>();
        private final Map<NodeStructure, SharedNode> nodesByStructure = new HashMap<>();
        private final NodeIdSequence nodeIds;

        private XmlWriteContext(
                List<Node> rootNodes,
                NodeIdSequence nodeIds) {
            this.nodeIds = nodeIds;
            for (Node rootNode : rootNodes) {
                register(rootNode);
            }
            Set<SharedNode> emittedNodes = new LinkedHashSet<>();
            for (Node rootNode : rootNodes) {
                planReferences(rootNode, emittedNodes);
            }
        }

        private SharedNode register(Node node) {
            List<SharedNode> childNodes = new ArrayList<>();
            if (node.isAmbiguous()) {
                for (Node childNode : node.childNodes) {
                    childNodes.add(register(childNode));
                }
            }

            Set<LeafElementType> completionCandidates = node.getCompletionCandidates();
            NodeStructure structure = new NodeStructure(
                    List.copyOf(node.tokenTypes),
                    List.copyOf(node.parseCandidates),
                    completionCandidates.size() <= maxCompletionCandidates ?
                            List.copyOf(completionCandidates) :
                            List.of(),
                    childNodes.stream().map(child -> child.structureId).toList());
            SharedNode sharedNode = nodesByStructure.get(structure);
            if (sharedNode == null) {
                sharedNode = new SharedNode(nodesByStructure.size());
                nodesByStructure.put(structure, sharedNode);
            }
            sharedNodes.put(node, sharedNode);
            return sharedNode;
        }

        private void planReferences(
                Node node,
                Set<SharedNode> emittedNodes) {
            SharedNode sharedNode = sharedNodes.get(node);
            if (!emittedNodes.add(sharedNode)) {
                sharedNode.referenced = true;
                return;
            }

            if (node.isAmbiguous()) {
                for (Node childNode : node.childNodes) {
                    planReferences(childNode, emittedNodes);
                }
            }
        }

        private String nextNodeId() {
            return nodeIds.next();
        }
    }

    private static final class NodeIdSequence {
        private int value;

        private String next() {
            String sequence = Integer.toString(value++);
            return "N" + "0".repeat(Math.max(0, 4 - sequence.length())) + sequence;
        }
    }

    private static final class SharedNode {
        private final int structureId;
        private boolean referenced;
        private String id;

        private SharedNode(int structureId) {
            this.structureId = structureId;
        }
    }

    private record NodeStructure(
            List<TokenType> tokenTypes,
            List<ElementTypeBase> parseCandidates,
            List<LeafElementType> completionCandidates,
            List<Integer> childStructureIds) {
    }
}
