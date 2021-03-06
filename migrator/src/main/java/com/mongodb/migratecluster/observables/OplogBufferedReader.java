package com.mongodb.migratecluster.observables;

import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.migratecluster.helpers.MongoDBHelper;
import com.mongodb.migratecluster.utils.Timer;
import io.reactivex.Observable;
import io.reactivex.Observer;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * File: OplogBufferedReader
 * Author: Shyam Arjarapu
 * Date: 1/14/19 9:50 AM
 * Description:
 *
 * A class to help read the oplog entries and notify the consumers
 * of documents read either in bulk or timer based mode.
 */
public class OplogBufferedReader extends Observable<List<Document>> {
    private final MongoClient client;
    private final BsonTimestamp lastTimeStamp;
    private final int BUFFER_SIZE = 1000;
    private final Object lockObject = new Object();

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Document> queue;

    final static Logger logger = LoggerFactory.getLogger(OplogBufferedReader.class);

    public OplogBufferedReader(MongoClient client, BsonTimestamp lastTimeStamp) {
        this.client = client;
        this.lastTimeStamp = lastTimeStamp;
        this.queue = new ConcurrentLinkedQueue<>();
        this.client.setReadPreference(ReadPreference.secondaryPreferred());
    }

    @Override
    protected void subscribeActual(Observer<? super List<Document>> observer) {
        MongoCollection<Document> collection =
                MongoDBHelper.getCollection(client, "local", "oplog.rs");

        Document query = getFindQuery();
        MongoCursor<Document> cursor =
                collection
                        .find(query)
                        .sort(new Document("$natural", 1))
                        .cursorType(CursorType.Tailable)
                        .cursorType(CursorType.TailableAwait)
                        .noCursorTimeout(true)
                        .iterator();

        // have a timer that notifies the collected items for every 5 seconds
        Timer timer = new Timer(5000);
        Runnable task = () -> {
            collectAndNotify(observer, "Elapsed Timer");
        };
        timer.schedule(task);
        while (cursor.hasNext()){
            Document document = cursor.next();

            // wait upon lock before adding
            synchronized (lockObject) {
                this.queue.add(document);
                counter.addAndGet(1);
            }

            if (this.queue.size() == BUFFER_SIZE) {
                collectAndNotify(observer, "Buffered Reader");
                // reset the timer for next notification as we just notified
                timer.reset();
            }
        }
    }

    /**
     * Notifies the consumer about all the collected items
     *
     * @param observer the consumer listening to the published events
     * @param invoker a string representing if invoker is timer based or counter based
     */
    private void collectAndNotify(Observer<? super List<Document>> observer, String invoker) {
        synchronized (lockObject) {
            List<Document> documents = this.queue.stream().collect(Collectors.toList());
            this.queue.clear();
            logger.info("collectAndNotify invoked by [{}] is notifying subscribers about [{}] documents. Total notified {}",
                    invoker, documents.size(), this.counter.get());
            if (documents == null) {
                documents = new ArrayList<>();
            }
            observer.onNext(documents);
        }
    }


    /**
     * Get's the filter for the find operation on oplog
     *
     * @return a document representing the filter for the query
     */
    private Document getFindQuery() {
        Document noOpFilter = new Document("op", new Document("$ne", "n"));
        if (lastTimeStamp == null) {
            return noOpFilter;
        }
        else{
            Document timestampFilter = new Document("ts", new Document("$gt", lastTimeStamp));
            List<Document> filters = new ArrayList<>();
            filters.add(noOpFilter);
            filters.add(timestampFilter);

            return new Document("$and", filters);
        }
    }

}
