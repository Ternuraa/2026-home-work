package company.vk.edu.distrib.compute.ternuraa.cluster;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

public final class RendezvousSharding {
    private static final String HASH_ALGORITHM = "SHA-256";

    private RendezvousSharding() {
    }

    public static String resolveOwner(String key, List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            throw new IllegalArgumentException("nodeIds must not be empty");
        }

        String owner = nodeIds.getFirst();
        long bestScore = score(key, owner);
        for (int i = 1; i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            long nodeScore = score(key, nodeId);
            if (nodeScore > bestScore) {
                bestScore = nodeScore;
                owner = nodeId;
            }
        }
        return owner;
    }

    public static List<String> sortedNodeIds(List<Integer> httpPorts) {
        return httpPorts.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .toList();
    }

    private static long score(String key, String nodeId) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest((key + ":" + nodeId).getBytes(StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hash algorithm unavailable: " + HASH_ALGORITHM, e);
        }
    }
}
