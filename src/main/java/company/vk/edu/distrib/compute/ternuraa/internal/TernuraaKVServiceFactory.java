package company.vk.edu.distrib.compute.ternuraa.internal;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import java.io.IOException;
import java.nio.file.Paths;

public class TernuraaKVServiceFactory extends KVServiceFactory {

    @Override
    protected KVService doCreate(int port) throws IOException {
        Dao<byte[]> dao = new TernuraaDao(Paths.get("data"));

        return new TernuraaKVService(dao);
    }
}
