# Гузориши санҷиш

| Санҷиш | Натиҷа |
|---|---|
| Gradle build / компилятсияи Kotlin | дар муҳити таҳия иҷро нашуд (Gradle, Kotlin, интернет нест); build-ро GitHub Actions иҷро мекунад |
| Unit-тестҳо (TextToolsTest, ReportGeneratorTest, DocxExporterTest) | **навишта шудаанд, иҷро нашудаанд** |
| Баландии қавсҳои `{}()[]` дар ҳамаи .kt | гузашт (скрипт) |
| YAML-и workflow | гузашт (PyYAML) |
| XML-и manifest/resources | гузашт (well-formed) |
| DOCX ва PPTX (экспортёрҳои Java) | **санҷида шуд**: файлҳои намунавӣ бо python-docx ва python-pptx кушода шуданд, LibreOffice онҳоро ба PDF табдил дод (матни тоҷикӣ, рақами саҳифа, слайдҳо бо сурат дар тарҳбандиҳои TITLE/TEXT/IMG_RIGHT/IMG_LEFT/FULL дурустанд) |
| PDF export, чоп, handwriting дар дастгоҳ | **санҷида нашудааст** |

Санҷиши воқеӣ: push ба GitHub → Actions (unit tests + lint + assembleDebug), баъд насб дар телефон.
Lint `abortOnError=false` аст, то ҳисобот сохта шавад; хатоҳои lint дар build-reports намоён мешаванд.
