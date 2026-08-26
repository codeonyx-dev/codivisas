# Retrofit necesita conservar firmas genéricas y anotaciones HTTP.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keep interface com.codeonyx.bcvaqui.data.api.** { *; }

# Modelos JSON usados por Retrofit y kotlinx.serialization.
# Son pocos y conservarlos evita fallos de deserialización en builds minificadas.
-keep class com.codeonyx.bcvaqui.data.model.** { *; }
-keepclassmembers class com.codeonyx.bcvaqui.data.model.** {
    *** Companion;
}
-keepclasseswithmembers,includedescriptorclasses class * {
    kotlinx.serialization.KSerializer serializer(...);
}
