package com.report.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private ElasticsearchVectorStore vectorStore;
    private ChatClient chatClient;

    public RagService(ElasticsearchVectorStore vectorStore, ChatClient.Builder clientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = clientBuilder.build();
    }

    public void ingestPDF(String filePath) {

        try {
            Resource resource = new FileSystemResource(filePath);
            PagePdfDocumentReader pdfReader =
                    new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.read();
            System.out.println("读取文件完成");
            // 添加文件名metadata
            String fileName = resource.getFilename();
            documents.forEach(doc -> {
                doc.getMetadata().put("file_name", fileName);
            });
            System.out.println("index索引名完成");
            // 文本切分
            documents = new TokenTextSplitter().apply(documents);
            System.out.println("切分后文档数: " + documents.size());
            documents.forEach(doc -> {
                System.out.println(doc.getMetadata());
            });
            vectorStore.doAdd(documents);
            System.out.println("向量存储完成");
        } catch (Exception e) {
            System.out.println("向量存储失败");
            e.printStackTrace();
        }
    }

    public String queryLLM(String question) {

        // Querying the vector store for documents related to the question
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.45)
                .build();

        List<Document> vectorStoreResult =
                vectorStore.doSimilaritySearch(request);

        // Merging the documents into a single string
        String documents = vectorStoreResult.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));


        // Setting the prompt with the context
        String prompt = """
       You're assisting with providing the rules of the tabletop game Runewars.
       Use the information from the DOCUMENTS section to provide accurate answers to the
       question in the QUESTION section.
       If unsure, simply state that you don't know.


       DOCUMENTS:
       """ + documents
                + """
       QUESTION:
       """ + question;




        // Calling the chat model with the question
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();


        if (vectorStoreResult.isEmpty()) {
            return "I don't know.";
        }

        return response +
                System.lineSeparator() +
                "Found at page: " +
                // Retrieving the first ranked page number from the document metadata
                vectorStoreResult.get(0).getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER) +
                " of the manual";
    }
}