import os, codecs


def tokenize_fix(filename):
    f = codecs.open(tokenizedDir + filename, "r", "utf-8")
    offset = (len(".tagged.tok")) * -1

    w = codecs.open(annotationDir+filename[:offset], "w", "utf-8")
    w2 = codecs.open(originalDir+filename[:offset], "w", "utf-8")

#    w = open(annotationDir+filename[(len(filename) - filename[::-1].find("/")):offset], "w+")
#    w2 = open(originalDir+filename[(len(filename) - filename[::-1].find("/")):offset], "w+")
    lines = f.readlines()
    brokenTableBeginTag = "< TABLE >"
    brokenCodeBeginTag = "< CODE >"
    brokenEquBeginTag = "< EQUATION >"
    brokenMiscBeginTag = "< MISCELLANEOUS >"

    brokenTableEndTag = "< / TABLE >"
    brokenCodeEndTag = "< / CODE >"
    brokenEquEndTag = "< / EQUATION >"
    brokenMiscEndTag = "< / MISCELLANEOUS >"

    tableBeginTag = "<TABLE>"
    codeBeginTag = "<CODE>"
    equBeginTag = "<EQUATION>"
    miscBeginTag = "<MISCELLANEOUS>"

    tableEndTag = "</TABLE>"
    codeEndTag = "</CODE>"
    equEndTag = "</EQUATION>"
    miscEndTag = "</MISCELLANEOUS>"

    tags = {brokenTableBeginTag, brokenCodeBeginTag,
            brokenEquBeginTag, brokenMiscBeginTag, brokenTableEndTag, brokenCodeEndTag,
            brokenEquEndTag, brokenMiscEndTag, tableBeginTag, codeBeginTag, equBeginTag, miscBeginTag,
            tableEndTag, codeEndTag, equEndTag, miscEndTag}
    l = ""

    for line in lines:
        if len(line) > 0:
            line = line.replace(brokenTableBeginTag, tableBeginTag)
            line = line.replace(brokenCodeBeginTag, codeBeginTag)
            line = line.replace(brokenEquBeginTag, equBeginTag)
            line = line.replace(brokenMiscBeginTag, miscBeginTag)

            line = line.replace(brokenTableEndTag, tableEndTag)
            line = line.replace(brokenCodeEndTag, codeEndTag)
            line = line.replace(brokenEquEndTag, equEndTag)
            line = line.replace(brokenMiscEndTag, miscEndTag)
            line = line.replace("< BR >", "\n")
            l = line
            w.write(l)
            for tag in tags:
                l = l.replace(tag, "")
            w2.write(l)

    w.close()
    w2.close()

    print("processed " + filename)



# taggedDir : the directory that contains annotation *.tagged files 
# tokenizedDir

# tagged | tokenized (should be temporary directory; delete after producing /annotation and /original)   
#        | annotation 
#        | original 

taggedDir = "/Users/mhjang/Desktop/clearnlp/tagged"
os.system("java com.clearnlp.run.Tokenizer -i " + taggedDir + " -ie .tagged")
tokenizedDir = taggedDir  + "/tokenized/"

# make a directory and move tokenized files 
if not os.path.exists(tokenizedDir):
    os.makedirs(tokenizedDir)

#print("find . -name \"*.tok\" -exec cp {} "+tokenizedDir+" \;")
os.system("find . -name \"*.tok\" -exec cp {} "+tokenizedDir+" \;")
os.system("find . -name \"*.tok\" -exec rm {} \;")

# make "original" and "annotation" directory
originalDir = taggedDir + "/original/"
if not os.path.exists(originalDir):
    os.makedirs(originalDir)

annotationDir = taggedDir + "/annotation/"
if not os.path.exists(annotationDir):
    os.makedirs(annotationDir)

filelist = os.listdir(tokenizedDir)
for file in filelist:
    print("opening " + str(tokenizedDir + file))
    if ".DS_Store" not in file:
        tokenize_fix(file)

parsedDir = taggedDir + "/parsed"
if not os.path.exists(parsedDir):
    os.makedirs(parsedDir)


#os.system("python /Users/mhjang/Desktop/clearnlp/tokenizerFix.py "+ tokenizedDir)
#os.system("java -XX:+UseConcMarkSweepGC -Xmx3g com.clearnlp.nlp.engine.NLPDecode -z dep -c /Users/mhjang/Desktop/clearnlp/configuration.xml -i "+originalDir + " -ie .txt")

print("find "+originalDir+"  -name \"*.cnlp\" -exec cp {} "+parsedDir + " \;")
os.system("find "+originalDir+"  -name \"*.cnlp\" -exec cp {} "+parsedDir + " \;")
os.system("find "+originalDir+"  -name \"*.cnlp\" -exec rm {} \;")

# delete the tok files
os.system("rm -r " + tokenizedDir)


# So, I ran ClearNLP tokenizer on the annotated files and now I have to fix all the broken file.

