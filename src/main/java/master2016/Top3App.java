package master2016;

import org.apache.storm.Config;
//import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

import java.util.HashMap;

/**
 * Created by Sophie on 11/17/16.
 */
public class Top3App {

    public static void main(String[] args) {

        if(args.length < 4) {
            System.out.println("Not enough parameters. There should be four parameters.");
            return;
        }


        System.out.println("Parameters:");

        // parse langList
        String langTokenListStr = args[0];
        String[] langTokenList = langTokenListStr.split(",");
        HashMap<String, String> langTokenDict = new HashMap<>(langTokenList.length * 2);
        for(String e : langTokenList) {
            String[] langToken = e.split(":");
            langTokenDict.put(langToken[0], langToken[1].toLowerCase());
        }

        System.out.println("langList: " + langTokenDict.toString());

        // parse Kafka Broker URL
        String kafkaBrokerURL = args[1];

        System.out.println("Kafka Broker URL: " + kafkaBrokerURL);

        // parse topology name
        String topologyName = args[2];

        System.out.println("topologyName: " + topologyName);

        // parse outputFolder
        String outputFolder = args[3];

        System.out.println("Output folder: " + outputFolder);



        // for debug use only. // TODO, change to previous piece of code
//        HashMap<String, String> langTokenDict = new HashMap<>(4);
//        langTokenDict.put("en", "house");
//        langTokenDict.put("es", "ordenador");
//
//        String kafkaBrokerURL = "138.4.110.141:41604";
//        String topologyName = "Topology";
//        String outputFolder = "~/";


        final String groupID = "YesWeCan";

        // build topology
        TopologyBuilder topologyBuilder = new TopologyBuilder();

        // the number of executors for each spout / bolt, and each executor has one task by default
        int parallelismHint = langTokenDict.size();

        topologyBuilder.setSpout("KafkaSpout", new KafkaSpout(langTokenDict, kafkaBrokerURL, groupID), parallelismHint);
        topologyBuilder.setBolt("Top3Bolt", new TwitterTopKBolt(langTokenDict, outputFolder, 3), parallelismHint)
                .fieldsGrouping("KafkaSpout", KafkaSpout.TWITTER_STREAM_NAME, new Fields(KafkaSpout.LANGUAGE_NAME));


        Config clusterConfig = new Config();
        // there are three workers in the cluster
        clusterConfig.setNumWorkers(3);

        // local model
//        LocalCluster locClu = new LocalCluster();
//        locClu.submitTopology(topologyName, new Config(), topologyBuilder.createTopology());
//        locClu.killTopology(topologyName);
//        locClu.shutdown();

        // submit topology to the cluster
        StormSubmitter submitter = new StormSubmitter();
        try {
            submitter.submitTopology(topologyName, clusterConfig, topologyBuilder.createTopology());
        } catch (AlreadyAliveException e) {
            e.printStackTrace();
        } catch (InvalidTopologyException e) {
            e.printStackTrace();
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }

        Utils.sleep(100000);

    }

}
