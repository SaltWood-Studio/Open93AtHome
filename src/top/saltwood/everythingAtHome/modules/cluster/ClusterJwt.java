package top.saltwood.everythingAtHome.modules.cluster;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

public class ClusterJwt {
    public static final String issuer = "SALTWOOD";
    public final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS512;
    public static SecretKey key = Jwts.SIG.HS512.key().build();
    public String clusterSecret;
    public Long expiration = 86400L;
    public String clusterId;
    
    public ClusterJwt(String clusterId, String clusterSecret) {
        this.clusterId = clusterId;
        this.clusterSecret = clusterSecret;
    }
    
    public static JwtBuilder generateJwtToken(String issuer, Long expiration,
                                              SecretKey key, SecureDigestAlgorithm<? super javax.crypto.SecretKey, ?> algorithm,
                                              String id) {
        return Jwts.builder()
                .claims()
                .add("cluster_id", id)
                .and()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(key, algorithm);
    }
    
    /**
     * 创建jwt
     *
     * @return 返回生成的jwt token
     */
    public String generateJwtToken() {
        
        return generateJwtToken(issuer, this.expiration, key, ALGORITHM, this.clusterId)
                .claims()
                .add("cluster_secret", this.clusterSecret)
                .and()
                .compact();
    }
}
