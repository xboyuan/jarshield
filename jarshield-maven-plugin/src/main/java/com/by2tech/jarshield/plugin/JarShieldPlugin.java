package com.by2tech.jarshield.plugin;

import com.by2tech.jarshield.Const;
import com.by2tech.jarshield.JarEncryptor;
import com.by2tech.jarshield.util.StrUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * 加密jar/war文件的maven插件
 *
 * @author roseboy
 */
@Mojo(name = "jarShield", defaultPhase = LifecyclePhase.PACKAGE)
public class JarShieldPlugin extends AbstractMojo {

    //MavenProject
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    //密码
    @Parameter(property = "jarshield.password", defaultValue = "#")
    private String password;
    //机器码
    @Parameter(property = "jarshield.code")
    private String code;
    //加密的内部-lib/jar名称
    @Parameter(property = "jarshield.libjars")
    private String libjars;
    //要加密的包名前缀
    @Parameter(property = "jarshield.packages")
    private String packages;
    //要加密的配置文件名
    @Parameter(property = "jarshield.cfgfiles")
    private String cfgfiles;
    //排除的类名
    @Parameter(property = "jarshield.excludes")
    private String excludes;
    //外部依赖jarlib
    @Parameter(property = "jarshield.classpath")
    private String classpath;
    //调试
    @Parameter(defaultValue = "false", property = "jarshield.debug")
    private Boolean debug;
    //是否跳过加密（兼容 ygqygq2:2.0.2 接口，便于在 pluginManagement 通过 profile 切换默认/启用）
    @Parameter(defaultValue = "true", property = "jar.encrypt.skip")
    private Boolean skip;

    /**
     * 打包的时候执行
     *
     * @throws MojoExecutionException MojoExecutionException
     * @throws MojoFailureException   MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log logger = getLog();
        if (Boolean.TRUE.equals(skip)) {
            logger.info("JarShield: encryption is skipped (skip=true)");
            return;
        }
        Const.DEBUG = debug;
        Build build = project.getBuild();

        long t1 = System.currentTimeMillis();

        String targetJar = build.getDirectory() + File.separator + build.getFinalName()
                + "." + project.getPackaging();
        logger.info("Encrypting " + project.getPackaging() + " [" + targetJar + "]");
        List<String> includeJarList = StrUtils.toList(libjars);
        List<String> packageList = StrUtils.toList(packages);
        List<String> excludeClassList = StrUtils.toList(excludes);
        List<String> classPathList = StrUtils.toList(classpath);
        List<String> cfgFileList = StrUtils.toList(cfgfiles);
        includeJarList.add("-");

        JarEncryptor encryptor = new JarEncryptor(targetJar, password.trim().toCharArray());
        encryptor.setCode(StrUtils.isEmpty(code) ? null : code.trim().toCharArray());
        encryptor.setPackages(packageList);
        encryptor.setIncludeJars(includeJarList);
        encryptor.setExcludeClass(excludeClassList);
        encryptor.setClassPath(classPathList);
        encryptor.setCfgfiles(cfgFileList);
        String result = encryptor.doEncryptJar();
        long t2 = System.currentTimeMillis();

        logger.info("Encrypt " + encryptor.getEncryptFileCount() + " classes");
        logger.info("Encrypted " + project.getPackaging() + " [" + result + "]");
        logger.info("Encrypt complete");
        logger.info("Time [" + ((t2 - t1) / 1000d) + " s]");
        logger.info("");
    }

}