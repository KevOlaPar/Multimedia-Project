from mimetypes import init
import os
from telnetlib import WILL
from tracemalloc import start
import matplotlib.pyplot as plt
import numpy as np
import cv2 as cv
from pprint import pprint
import scipy.ndimage as nd

class Hist:

    def hist(hsv):
        hist = cv.calcHist([hsv],[0, 1], None, [50,100], [1, 180, 0, 256])
        hist = cv.normalize(hist, hist, alpha=0, beta=1, norm_type=cv.NORM_MINMAX)
        return hist
        

class Frame:

    def __init__(self, rgb):
        self.rgb = rgb
        self.hsv = cv.cvtColor(rgb,cv.COLOR_RGB2HSV)
        
        self.blocks = []
        startw = 15
        starth = 0
        blockw = 150
        blockh = 90
        self.fact = 150*90

        for i in range(3):
            for j in range(3):
                sh = blockh*i + starth
                sw = blockw*j + startw
                block = self.hsv[sh:sh+blockh,sw:sw+blockw,:]
                self.blocks.append(block)
        self.hist = Hist.hist(self.hsv)

class VideoReader:

    def __init__(self, path):
        self.path = path
        self.w = 480
        self.h = 270
        self.sz = 480*270
        self.totalFrames = os.path.getsize(self.path)//(self.sz*3);
        self.seconds = 6
        # self.seconds = self.totalFrames//30
        self.file = open(path, "rb")

    def getFrame(self, frameNo):
        self.file.seek(frameNo * 3 * self.sz)
        bytes = self.file.read(3 * self.sz)
        rgb = np.zeros((self.h, self.w, 3), dtype=np.uint8)

        pix = 0
        for r in range(self.h):
            for c in range(self.w):
                rgb[r][c][0] = bytes[pix]
                rgb[r][c][1] = bytes[pix + self.sz]
                rgb[r][c][2] = bytes[pix + self.sz * 2]
                pix+=1
                
        return Frame(rgb)

    
    def showFrame(self, frameNo):
        plt.imshow(self.getFrame(frameNo).rgb)
        plt.show()


class Logo:

    def __init__(self, path):
        self.path = path
        self.img = cv.imread(path)
        self.hsv = cv.cvtColor(self.img,cv.COLOR_BGR2HSV)
        self.hist = Hist.hist(self.hsv)
    
    def showLogo(self):
        plt.imshow(cv.cvtColor(self.img, cv.COLOR_BGR2RGB))
        plt.show()


class Detector:

    def __init__(self, videoPath, logosPath):
        self.reader = VideoReader(videoPath)
        self.logos = []
        self.imageProcessor = ImageProcessor()
        for logoPath in logosPath:
            self.logos.append(Logo(logoPath))

    def mean(self, confirmations):
        frame = confirmations[0][1]
        s = 0
        for v, _, _ in confirmations:
            s+=v
        return (s/len(confirmations), frame)

    def bestConfirmation(self, confirmations, overFrames):
        logoslen = len(confirmations)
        totalFrames = len(confirmations[0])

        frames = totalFrames - overFrames + 1
        bestConfirmation = [(100000000000,0) for i in range(logoslen)]
        consConfirmations = [[ (0, 0) for _ in range(frames)] for _ in range(logoslen)]

        for i in range(logoslen):
            c = confirmations[i]
            for j in range(frames):
                consConfirmations[i][j] = self.mean(c[j:j+overFrames])
                if bestConfirmation[i][0] > consConfirmations[i][j][0]:
                    bestConfirmation[i] = consConfirmations[i][j]
        return bestConfirmation


    def detect(self):
        totalSeconds = self.reader.seconds
        skipFrames = 10
        logoslen = len(self.logos)

        confirmations = [[] for i in range(logoslen)]
        sec = 0
        for sec in range(totalSeconds):
            print("start detection for sec ", sec)
            startFrame = sec*30
            for i in range(startFrame, startFrame + 30, skipFrames):
                frame = self.reader.getFrame(i)
                for j, logo in enumerate(self.logos):
                    confirmations[j].append((self.imageProcessor.logoPresent(frame, logo), i, i//30))

        pprint(confirmations)

        self.bestConfirmation = self.bestConfirmation(confirmations, 10)
        return self.bestConfirmation
            


                    

class ImageProcessor:

    def compareHist(self, h1, h2):
        
        # masked = h1 > 0
        # print(masked)
        # h1 = np.extract(masked, h1)
        # h2 = np.extract(masked, h2)
        
        # print(h1, h2)

        test1 = cv.compareHist(h1, h2, 0)
        test2 = cv.compareHist(h1,h2, 1)
        test3 = cv.compareHist(h1,h2, 2)
        test4 = cv.compareHist(h1,h2, 3)
        print(test1, test2, test3, test4)
        return test3


    def logoPresent(self, frame, logo):
        test1 = cv.compareHist(frame.hist,logo.hist, 0)
        test2 = cv.compareHist(frame.hist,logo.hist, 1)
        test3 = cv.compareHist(frame.hist,logo.hist, 2)
        test4 = cv.compareHist(frame.hist,logo.hist, 3)
        print(test1, test2, test3, test4)
        # test = self.compareHist(logo.hist, frame.hist)

        return test3

detector = Detector("/Users/parthivmangukiya/Downloads/dataset/Videos/data_test1.rgb", ["/Users/parthivmangukiya/Downloads/dataset/Brand Images/starbucks_logo.bmp", "/Users/parthivmangukiya/Downloads/dataset/Brand Images/subway_logo.bmp"])
# detectedLogos = detector.detect()
# print(detectedLogos)


# for i, c in enumerate(detectedLogos):
#     sec = c[1]//30
#     rows = 1
#     cols = 3
#     fig = plt.figure()
#     for j in range(cols):
#         fig.add_subplot(rows, cols, j+1)
#         plt.imshow(detector.reader.getFrame((sec+j)*30).rgb)
#         plt.title("logo " + str(i) + " sec " + str(sec + j))

#     plt.show()






#########################################################

# fig = plt.figure()
# plt.axis('off')

# def analyseFrame(i, sec):
    
#     print("analysing for ", sec)
#     fig.add_subplot(3, 3, i*3 + 1)
#     frame = detector.reader.getFrame(sec*30)
#     plt.imshow(frame.rgb)
#     plt.title(sec)

#     fig.add_subplot(3, 3, i*3 + 2)
#     plt.plot(detector.logos[0].hist)
#     plt.plot(frame.hist)

#     fig.add_subplot(3, 3, i*3 + 3)
#     plt.plot(detector.logos[1].hist)
#     plt.plot(frame.hist)

#     t1 = detector.imageProcessor.logoPresent(frame, detector.logos[0])
#     t2 = detector.imageProcessor.logoPresent(frame, detector.logos[1])

#     print(t1, t2)
#     i+=1

# analyseFrame(0, 217)

# plt.show()

#######################


fig = plt.figure()
plt.axis('off')

img = detector.logos[1].img
hsv = detector.logos[1].hsv

# hist = cv.normalize(hist, hist, alpha=0, beta=1, norm_type=cv.NORM_MINMAX)

sec = 60

frame = detector.reader.getFrame(sec*30)

wl = (0, 0, 150)
wh = (255, 20, 255)
yl = (20, 0, 0)
yh = (40, 255, 255)


hsvWhiteMask = cv.inRange(hsv, wl, wh)
hsvYellowMask = cv.inRange(hsv, yl, yh) - hsvWhiteMask
hsv = cv.bitwise_and(hsv, hsv, mask=hsvYellowMask)
hist = cv.calcHist([hsv],[0], None, [20], [1, 180])
whites = np.count_nonzero(hsvWhiteMask)
hist = np.append(hist, [whites]).astype('float32')
hist/=(270*480)

fig.add_subplot(4, 3, 10)
plt.plot(hist, 'r')

fig.add_subplot(4, 3, 11)
plt.imshow(hsv)

# hist = cv.normalize()

for i in range(9):
    image = frame.blocks[i]

    hsvWhiteMask = cv.inRange(image, wl, wh)
    hsvYellowMask = cv.inRange(image, yl, yh) - hsvWhiteMask

    whites = np.count_nonzero(hsvWhiteMask)

    mask = hsvYellowMask + hsvWhiteMask
    image = cv.bitwise_and(image, image, mask=hsvYellowMask)
    histf = cv.calcHist([image],[0], None, [20], [1, 180])
    histf = np.append(histf, [whites]).astype('float32')

    histf /= frame.fact
    # print(histf)
    fig.add_subplot(4, 3, i+1)
    i2 = cv.cvtColor(image, cv.COLOR_HSV2RGB)
    plt.imshow(i2)
    print(i+1, detector.imageProcessor.compareHist(hist, histf))
    # fig.add_subplot(4, 3, i+2)
    # plt.plot(histf, 'r')

plt.show()