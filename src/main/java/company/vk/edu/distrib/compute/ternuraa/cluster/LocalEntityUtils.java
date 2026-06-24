package company.vk.edu.distrib.compute.ternuraa.cluster;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.NoSuchElementException;

public final class LocalEntityUtils {
    private LocalEntityUtils() {
    }

    public static EntityResult get(Dao<byte[]> dao, String id) throws IOException {
        try {
            byte[] value = dao.get(id);
            return new EntityResult(200, value);
        } catch (NoSuchElementException e) {
            return EntityResult.of(404);
        }
    }

    public static EntityResult put(Dao<byte[]> dao, String id, byte[] value) throws IOException {
        dao.upsert(id, value);
        return EntityResult.of(201);
    }

    public static EntityResult delete(Dao<byte[]> dao, String id) throws IOException {
        dao.delete(id);
        return EntityResult.of(202);
    }
}
