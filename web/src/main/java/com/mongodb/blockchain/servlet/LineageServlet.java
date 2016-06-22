package com.mongodb.blockchain.servlet;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Created by brein on 6/18/2016.
 */
public class LineageServlet extends HttpServlet {


    static Logger log = Logger.getLogger( LineageServlet.class.getName() );

    private String name;
    private MongoCollection<Document> transactions;
    private MongoDatabase db;
    private String  json;


    private String readFile( String path ) throws IOException {

        StringBuffer buf = new StringBuffer();

        RandomAccessFile aFile = new RandomAccessFile (path, "r");
        FileChannel inChannel = aFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while(inChannel.read(buffer) > 0)
        {
            buffer.flip();
            for (int i = 0; i < buffer.limit(); i++)
            {
                buf.append((char) buffer.get());
            }
            buffer.clear(); // do something with the data and clear/compact it.
        }
        inChannel.close();
        aFile.close();
        return buf.toString();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        name = config.getServletName();

        final MongoClient client = (MongoClient) config.getServletContext().getAttribute( "mongoclient" );
        db = client.getDatabase( config.getInitParameter( "db" ) );

        String commandFile =  config.getServletContext().getRealPath("/WEB-INF/aggregations/" + name + ".json" );
        try {
            json = readFile( commandFile );
        } catch (IOException x) {
            String err = "Failed to read command file "+commandFile;
            log.severe( err );
            throw new ServletException( err, x );
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        boolean error = false;

        String txID = req.getParameter( "tx" );
        json = json.replaceAll( "txId", txID );

        Document d = db.runCommand( Document.parse( json ) );
        List<Document> groups =  (List<Document>) d.get( "result" );

        try {
            resp.getWriter().print( db.runCommand( Document.parse( json ) ).toJson() ) ;
        } catch (IOException e) {
            log.warning( "Cant print response, for reason "+e.getMessage() );
            error = true;
        }

        if( error )
            resp.sendError( SC_INTERNAL_SERVER_ERROR  );

    }

    public String reformat( Document doc ) {

        List<Document> generations = (List<Document>) doc.get( "result" );

        for( int depth = 0; depth < generations.size(); depth++ ) {

            List<Document> transactions = (List<Document>)generations.get( depth ).get( "transactions" );
            for( int i = 0; i < transactions.size(); i++ ) {

            }
        }
        return null;
    }
}
