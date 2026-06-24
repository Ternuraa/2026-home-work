package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.KVCluster;
import company.vk.edu.distrib.compute.KVClusterFactory;

import java.util.List;

public final class KVClusterFactoryImpl extends KVClusterFactory {
    @Override
    protected KVCluster doCreate(List<Integer> ports) {
        return new KVClusterImpl(ports);
    }
}
