
from inspect import FrameInfo
from mimetypes import init
import os
from telnetlib import WILL
from tracemalloc import start
import matplotlib.pyplot as plt
import numpy as np
import cv2 as cv
from pprint import pprint
import scipy.ndimage as nd
from google.cloud import vision
import json

class Frame:

    def __init__(self, rgb):
        self.rgb = rgb

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

class Result:

    def __init__(self, score, boundry, frameNo):
        self.score = score
        self.boundry = boundry
        self.frameNo = frameNo

    @classmethod
    def fromGoogle(cls, frameNo, logo):
        return cls(logo.score, Boundry.fromGoogle(logo.bounding_poly), frameNo)

    def to_dict(r):
        result = dict()
        result["score"] = r.score
        result["boundry"] = r.boundry.to_dict()
        result["frameNo"] = r.frameNo
        return result

    @classmethod
    def fromDict(cls, d):
        return cls(d["score"], Boundry.fromDict(d["boundry"]), d["frameNo"])

class Boundry:
    def __init__(self, tl, tr, bl, br):
        self.tl = tl
        self.tr = tr
        self.bl = bl
        self.br = br

    @classmethod
    def fromDict(cls, d):
        tl = (d["tl"][0], d["tl"][1])
        tr = (d["tr"][0], d["tr"][1])
        bl = (d["bl"][0], d["bl"][1])
        br = (d["br"][0], d["br"][1])
        return cls(tl, tr, bl, br)

    @classmethod
    def fromGoogle(cls, boundry):
        tl = (boundry.vertices[0].x, boundry.vertices[0].y)
        tr = (boundry.vertices[1].x, boundry.vertices[1].y)
        bl = (boundry.vertices[2].x, boundry.vertices[2].y)
        br = (boundry.vertices[3].x, boundry.vertices[3].y)
        return cls(tl, tr, bl, br)

    def to_dict(b):
        result = dict()
        result["tl"] = list(b.tl)
        result["tr"] = list(b.tr)
        result["bl"] = list(b.bl)
        result["br"] = list(b.br)
        return result

class Detector:

    def __init__(self, videoPath, logos):
        self.reader = VideoReader(videoPath)
        self.client = vision.ImageAnnotatorClient()
        self.logos = logos
        self.results = dict()
        for logo in logos:
            self.results[logo] = []

    def parseLogos(self, frameNo, logos):
        if len(logos) > 0 and logos[0].description in self.logos:
            name = logos[0].description
            print(name)
            self.results[name].append(Result.fromGoogle(frameNo, logos[0]))
            

    def detectLogoInFrame(self, frameNo):
        image = self.reader.getFrame(frameNo).rgb
        content = cv.imencode('.jpg', image)[1].tobytes()

        content = vision.Image(content=content)

        response = self.client.logo_detection(image=content)
        logos = response.logo_annotations

        if response.error.message:
            raise Exception(
                '{}\nFor more info on error messages, check: '
                'https://cloud.google.com/apis/design/errors'.format(
                    response.error.message))

        self.parseLogos(frameNo, logos)

        return logos

    def detect(self):
        # self.detectLogoInFrame(180*30)
        # print(self.toJson())

        for frame in range(0, 9000, 30):
            print("Detection in frame ", frame)
            self.detectLogoInFrame(frame)

    def toJson(self):
        d = dict()
        for logo in self.results:
            d[logo] = []
            for r in  self.results[logo]:
                d[logo].append(r.to_dict())
        return json.dumps(d)

    def loadJson(self, j):
        d = json.loads(j)
        for logo in d:
            self.results[logo] = []
            for result in d[logo]:
                self.results[logo].append(Result.fromDict(result))


detector = Detector("/Users/parthivmangukiya/Downloads/dataset/Videos/data_test1.rgb", ["Starbucks", "Subway"])
# detector.detect()
# print(detector.toJson())

j = '{"Starbucks": [{"score": 0.9608190059661865, "boundry": {"tl": [101, 36], "tr": [129, 36], "bl": [129, 63], "br": [101, 63]}, "frameNo": 5040}, {"score": 0.9512998461723328, "boundry": {"tl": [192, 75], "tr": [219, 75], "bl": [219, 101], "br": [192, 101]}, "frameNo": 5070}, {"score": 0.9601954817771912, "boundry": {"tl": [237, 83], "tr": [265, 83], "bl": [265, 111], "br": [237, 111]}, "frameNo": 5100}, {"score": 0.9631644487380981, "boundry": {"tl": [271, 90], "tr": [300, 90], "bl": [300, 118], "br": [271, 118]}, "frameNo": 5130}, {"score": 0.9612778425216675, "boundry": {"tl": [269, 103], "tr": [298, 103], "bl": [298, 133], "br": [269, 133]}, "frameNo": 5160}, {"score": 0.9584997892379761, "boundry": {"tl": [243, 101], "tr": [273, 101], "bl": [273, 132], "br": [243, 132]}, "frameNo": 5190}, {"score": 0.9751725792884827, "boundry": {"tl": [229, 101], "tr": [261, 101], "bl": [261, 133], "br": [229, 133]}, "frameNo": 5220}, {"score": 0.9844807386398315, "boundry": {"tl": [238, 94], "tr": [274, 94], "bl": [274, 129], "br": [238, 129]}, "frameNo": 5250}, {"score": 0.9780346751213074, "boundry": {"tl": [232, 104], "tr": [273, 104], "bl": [273, 143], "br": [232, 143]}, "frameNo": 5280}, {"score": 0.9917368292808533, "boundry": {"tl": [232, 105], "tr": [273, 105], "bl": [273, 146], "br": [232, 146]}, "frameNo": 5310}, {"score": 0.9954432845115662, "boundry": {"tl": [236, 118], "tr": [282, 118], "bl": [282, 161], "br": [236, 161]}, "frameNo": 5340}, {"score": 0.9948830604553223, "boundry": {"tl": [235, 104], "tr": [282, 104], "bl": [282, 148], "br": [235, 148]}, "frameNo": 5370}, {"score": 0.996112048625946, "boundry": {"tl": [252, 100], "tr": [300, 100], "bl": [300, 146], "br": [252, 146]}, "frameNo": 5400}, {"score": 0.9949465990066528, "boundry": {"tl": [290, 102], "tr": [341, 102], "bl": [341, 150], "br": [290, 150]}, "frameNo": 5430}, {"score": 0.9826891422271729, "boundry": {"tl": [421, 36], "tr": [478, 36], "bl": [478, 99], "br": [421, 99]}, "frameNo": 5460}], "Subway": [{"score": 0.6993291974067688, "boundry": {"tl": [73, 212], "tr": [162, 212], "bl": [162, 236], "br": [73, 236]}, "frameNo": 1920}, {"score": 0.7751271724700928, "boundry": {"tl": [27, 191], "tr": [130, 191], "bl": [130, 217], "br": [27, 217]}, "frameNo": 1950}, {"score": 0.7062610387802124, "boundry": {"tl": [22, 155], "tr": [120, 155], "bl": [120, 184], "br": [22, 184]}, "frameNo": 1980}, {"score": 0.8917880654335022, "boundry": {"tl": [0, 23], "tr": [269, 23], "bl": [269, 79], "br": [0, 79]}, "frameNo": 2040}, {"score": 0.9234293699264526, "boundry": {"tl": [100, 44], "tr": [391, 44], "bl": [391, 128], "br": [100, 128]}, "frameNo": 2070}, {"score": 0.7883803844451904, "boundry": {"tl": [201, 211], "tr": [268, 211], "bl": [268, 233], "br": [201, 233]}, "frameNo": 2100}, {"score": 0.8830807209014893, "boundry": {"tl": [5, 15], "tr": [313, 15], "bl": [313, 78], "br": [5, 78]}, "frameNo": 2130}, {"score": 0.7278605699539185, "boundry": {"tl": [60, 146], "tr": [139, 146], "bl": [139, 168], "br": [60, 168]}, "frameNo": 2160}]}'
detector.loadJson(j)
print(detector.toJson())
