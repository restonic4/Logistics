package com.restonic4.ccpreprocess

/**
 * CCPreprocessExtension
 *
 * Optional DSL block to override defaults:
 *
 *   ccpreprocess {
 *       minecraftVersion = '1.21.1'   // override auto-detect from gradle.properties
 *       loader           = 'forge'    // override auto-detect from project name
 *       javaVersion      = '21'       // override java_version property
 *       extra            = [myVar: 'someValue']   // add custom variables
 *   }
 */
class CCPreprocessExtension {
    /** Override minecraft_version from gradle.properties */
    String minecraftVersion = null

    /** Override loader detection (fabric / forge / neoforge / common) */
    String loader = null

    /** Override java_version from gradle.properties */
    String javaVersion = null

    /** Extra context variables usable in //CC conditions */
    Map<String, String> extra = [:]
}