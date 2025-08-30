# 解压所有需要的jar包
mkdir -p target/
for jarfile in plugin/build/idea-sandbox/plugins-test/intellij-pdf-viewer/lib/*.jar; do
    jar -xf "$jarfile"
done

# 创建新的fat jar
jar -cf target/intellij-pdf-viewer.jar .
