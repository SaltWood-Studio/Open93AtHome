package modules.cluster;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.*;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClusterJwt {
    public String clusterSecret;
    public Long expiration = 86400L;
    public static final String issuer = "SALTWOOD";
    public String clusterId;
    public static SecretKey key = Jwts.SIG.HS512.key().build();
    public final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS512;

    public ClusterJwt(String clusterId, String clusterSecret){
        this.clusterId = clusterId;
        this.clusterSecret = clusterSecret;
    }

    /**
     * 创建jwt
     * @return
     *      返回生成的jwt token
     */
    public String generateJwtToken(){
        Map<String,Object> claims = new HashMap<>();
        claims.put("cluster_id", this.clusterId);
        claims.put("cluster_secret", this.clusterSecret);

        return generateJwtToken(issuer, this.expiration, this.key, ALGORITHM, this.clusterId)
                .claims(claims)
                .compact();
    }

    public static JwtBuilder generateJwtToken(String issuer, Long expiration,
                                              SecretKey key, SecureDigestAlgorithm algorithm,
                                              String subject) {
        return Jwts.builder()
                .header()
                .and()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .subject(subject)
                .signWith(key, algorithm);
    }
}
