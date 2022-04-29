import os
import matplotlib.pyplot as plt
import numpy as np
import cv2 as cv
from google.cloud import vision
import json
import sys
from PIL import Image as im

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
        br = (boundry.vertices[2].x, boundry.vertices[2].y)
        bl = (boundry.vertices[3].x, boundry.vertices[3].y)
        return cls(tl, tr, bl, br)

    def to_dict(b):
        result = dict()
        result["tl"] = list(b.tl)
        result["tr"] = list(b.tr)
        result["bl"] = list(b.bl)
        result["br"] = list(b.br)
        return result

    def to_final_dict(b):
        result = [list(b.tl), list(b.tr), list(b.bl), list(b.br)]
        return result

class Detector:

    def __init__(self, videoPath, logos):
        self.reader = VideoReader(videoPath)
        self.client = vision.ImageAnnotatorClient()
        self.logos = logos
        self.results = dict()
        self.STEPS = 5
        self.frames = dict()
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

    def detectInBetweenFrames(self, startFrame, endFrame):
        for frame in range(startFrame+self.STEPS, endFrame, self.STEPS):
            print("Detection in frame ", frame)
            self.detectLogoInFrame(frame)


    def intersect(self, intervals):
        intervals.sort()
        for i in range(1, len(intervals)):
            if intervals[i][1] <= intervals[i- 1][1]:
                return True
        return False

    def detect(self):
        # self.detectLogoInFrame(180*30)
        # print(self.toJson())

        # for frame in range(0, 9000, 30):
        #     print("Detection in frame ", frame)
        #     self.detectLogoInFrame(frame)

        # intervals = []
        # for logo, results in self.results.items():
        #     results.sort(key = lambda x: x.frameNo)
        #     start = results[0].frameNo
        #     end = results[-1].frameNo
        #     intervals.append((start, end))

        # if(self.intersect(intervals)):
        #     sys.exit("FATAL ERROR: logos are intersecting")
        
        # self.results = dict()
        # for logo in self.logos:
        #     self.results[logo] = []

        # for interval in intervals:
        #     self.detectInBetweenFrames(interval[0]-15, interval[1] + 15)

        self.interpolate()

    def interpolate(self):
        for _, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)
        
        for _, results in self.results.items():
            for i in range(len(results)-1):
                if results[i+1].frameNo - results[i].frameNo <= self.STEPS:
                    self.interpolateUtil(results, results[i+1], results[i])
        
        for _, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)

    def interpolateUtil(self, r, e, s):
        framesToInsert = e.frameNo - s.frameNo - 1
        if framesToInsert <= 0:
            return
        bs = s.boundry
        be = e.boundry
        tls = self.interpolateValues(bs.tl, be.tl, framesToInsert)
        trs = self.interpolateValues(bs.tr, be.tr, framesToInsert)
        bls = self.interpolateValues(bs.bl, be.bl, framesToInsert)
        brs = self.interpolateValues(bs.br, be.br, framesToInsert)
        for i in range(framesToInsert):
            r.append(Result(s.score, Boundry(tls[i], trs[i], bls[i], brs[i]), s.frameNo + i + 1))
        
    def interpolateValues(self, s, e, values):
        inter = values + 1
        changex = (e[0]-s[0])/inter
        changey = (e[1]-s[1])/inter
        v = []
        x = s[0]
        y = s[1]
        for _ in range(values):
            x+=changex
            y+=changey
            v.append((int(x), int(y))) 
        return v


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

    def toFinalJson(self):
        fj = dict()

        # Delete empty lists
        for logo in list(self.results.keys()):
            if len(self.results[logo]) == 0:
                del self.results[logo]

        for logo, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)

        fj["logos"] = []
        
        for logo, results in self.results.items():
            startFrame = results[0].frameNo
            fj["logos"].append({logo: startFrame})
        
        d = dict()

        for logo, results in self.results.items():
            for r in results:
                if r.frameNo not in d:
                    d[r.frameNo] = r.boundry.to_final_dict()
        
        d = dict(sorted(d.items()))
        fj["frames"] = d

        return json.dumps(fj)

    def fillFrames(self):
        for results in self.results.values():
            for r in results:
                self.frames[r.frameNo] = r.boundry

    def getFrame(self, frameNo):
        frame = self.reader.getFrame(frameNo).rgb
        if frameNo in self.frames:
            b = self.frames[frameNo]
            cv.rectangle(frame, b.tl, b.bl, (0,255,0), 2)            
        return frame



detector = Detector("/Users/parthivmangukiya/Downloads/dataset/Videos/data_test1.rgb", ["Starbucks", "Subway"])

# j = '{"Starbucks": [{"score": 0.9608190059661865, "boundry": {"tl": [101, 36], "tr": [129, 36], "bl": [129, 63], "br": [101, 63]}, "frameNo": 5040}, {"score": 0.9512998461723328, "boundry": {"tl": [192, 75], "tr": [219, 75], "bl": [219, 101], "br": [192, 101]}, "frameNo": 5070}, {"score": 0.9601954817771912, "boundry": {"tl": [237, 83], "tr": [265, 83], "bl": [265, 111], "br": [237, 111]}, "frameNo": 5100}, {"score": 0.9631644487380981, "boundry": {"tl": [271, 90], "tr": [300, 90], "bl": [300, 118], "br": [271, 118]}, "frameNo": 5130}, {"score": 0.9612778425216675, "boundry": {"tl": [269, 103], "tr": [298, 103], "bl": [298, 133], "br": [269, 133]}, "frameNo": 5160}, {"score": 0.9584997892379761, "boundry": {"tl": [243, 101], "tr": [273, 101], "bl": [273, 132], "br": [243, 132]}, "frameNo": 5190}, {"score": 0.9751725792884827, "boundry": {"tl": [229, 101], "tr": [261, 101], "bl": [261, 133], "br": [229, 133]}, "frameNo": 5220}, {"score": 0.9844807386398315, "boundry": {"tl": [238, 94], "tr": [274, 94], "bl": [274, 129], "br": [238, 129]}, "frameNo": 5250}, {"score": 0.9780346751213074, "boundry": {"tl": [232, 104], "tr": [273, 104], "bl": [273, 143], "br": [232, 143]}, "frameNo": 5280}, {"score": 0.9917368292808533, "boundry": {"tl": [232, 105], "tr": [273, 105], "bl": [273, 146], "br": [232, 146]}, "frameNo": 5310}, {"score": 0.9954432845115662, "boundry": {"tl": [236, 118], "tr": [282, 118], "bl": [282, 161], "br": [236, 161]}, "frameNo": 5340}, {"score": 0.9948830604553223, "boundry": {"tl": [235, 104], "tr": [282, 104], "bl": [282, 148], "br": [235, 148]}, "frameNo": 5370}, {"score": 0.996112048625946, "boundry": {"tl": [252, 100], "tr": [300, 100], "bl": [300, 146], "br": [252, 146]}, "frameNo": 5400}, {"score": 0.9949465990066528, "boundry": {"tl": [290, 102], "tr": [341, 102], "bl": [341, 150], "br": [290, 150]}, "frameNo": 5430}, {"score": 0.9826891422271729, "boundry": {"tl": [421, 36], "tr": [478, 36], "bl": [478, 99], "br": [421, 99]}, "frameNo": 5460}], "Subway": [{"score": 0.6993291974067688, "boundry": {"tl": [73, 212], "tr": [162, 212], "bl": [162, 236], "br": [73, 236]}, "frameNo": 1920}, {"score": 0.7751271724700928, "boundry": {"tl": [27, 191], "tr": [130, 191], "bl": [130, 217], "br": [27, 217]}, "frameNo": 1950}, {"score": 0.7062610387802124, "boundry": {"tl": [22, 155], "tr": [120, 155], "bl": [120, 184], "br": [22, 184]}, "frameNo": 1980}, {"score": 0.8917880654335022, "boundry": {"tl": [0, 23], "tr": [269, 23], "bl": [269, 79], "br": [0, 79]}, "frameNo": 2040}, {"score": 0.9234293699264526, "boundry": {"tl": [100, 44], "tr": [391, 44], "bl": [391, 128], "br": [100, 128]}, "frameNo": 2070}, {"score": 0.7883803844451904, "boundry": {"tl": [201, 211], "tr": [268, 211], "bl": [268, 233], "br": [201, 233]}, "frameNo": 2100}, {"score": 0.8830807209014893, "boundry": {"tl": [5, 15], "tr": [313, 15], "bl": [313, 78], "br": [5, 78]}, "frameNo": 2130}, {"score": 0.7278605699539185, "boundry": {"tl": [60, 146], "tr": [139, 146], "bl": [139, 168], "br": [60, 168]}, "frameNo": 2160}]}'

ij = '{"Starbucks": [{"score": 0.9424941539764404, "boundry": {"tl": [104, 35], "tr": [132, 35], "bl": [132, 61], "br": [104, 61]}, "frameNo": 5030}, {"score": 0.9460070133209229, "boundry": {"tl": [114, 42], "tr": [140, 42], "bl": [140, 68], "br": [114, 68]}, "frameNo": 5035}, {"score": 0.9608190059661865, "boundry": {"tl": [101, 36], "tr": [129, 36], "bl": [129, 63], "br": [101, 63]}, "frameNo": 5040}, {"score": 0.9620167016983032, "boundry": {"tl": [105, 38], "tr": [130, 38], "bl": [130, 64], "br": [105, 64]}, "frameNo": 5045}, {"score": 0.9596078991889954, "boundry": {"tl": [111, 52], "tr": [140, 52], "bl": [140, 79], "br": [111, 79]}, "frameNo": 5050}, {"score": 0.9658350944519043, "boundry": {"tl": [150, 63], "tr": [176, 63], "bl": [176, 88], "br": [150, 88]}, "frameNo": 5055}, {"score": 0.9646032452583313, "boundry": {"tl": [174, 56], "tr": [200, 56], "bl": [200, 81], "br": [174, 81]}, "frameNo": 5060}, {"score": 0.9598027467727661, "boundry": {"tl": [184, 61], "tr": [211, 61], "bl": [211, 87], "br": [184, 87]}, "frameNo": 5065}, {"score": 0.9512998461723328, "boundry": {"tl": [192, 75], "tr": [219, 75], "bl": [219, 101], "br": [192, 101]}, "frameNo": 5070}, {"score": 0.9443264007568359, "boundry": {"tl": [189, 71], "tr": [216, 71], "bl": [216, 97], "br": [189, 97]}, "frameNo": 5075}, {"score": 0.9645161628723145, "boundry": {"tl": [196, 72], "tr": [224, 72], "bl": [224, 98], "br": [196, 98]}, "frameNo": 5080}, {"score": 0.9334040880203247, "boundry": {"tl": [206, 81], "tr": [233, 81], "bl": [233, 107], "br": [206, 107]}, "frameNo": 5085}, {"score": 0.963314414024353, "boundry": {"tl": [226, 84], "tr": [253, 84], "bl": [253, 109], "br": [226, 109]}, "frameNo": 5090}, {"score": 0.958935558795929, "boundry": {"tl": [235, 78], "tr": [263, 78], "bl": [263, 105], "br": [235, 105]}, "frameNo": 5095}, {"score": 0.9601954817771912, "boundry": {"tl": [237, 83], "tr": [265, 83], "bl": [265, 111], "br": [237, 111]}, "frameNo": 5100}, {"score": 0.9459146857261658, "boundry": {"tl": [242, 87], "tr": [270, 87], "bl": [270, 114], "br": [242, 114]}, "frameNo": 5105}, {"score": 0.9565145969390869, "boundry": {"tl": [232, 79], "tr": [261, 79], "bl": [261, 107], "br": [232, 107]}, "frameNo": 5110}, {"score": 0.9604263305664062, "boundry": {"tl": [234, 82], "tr": [263, 82], "bl": [263, 110], "br": [234, 110]}, "frameNo": 5115}, {"score": 0.9297158718109131, "boundry": {"tl": [249, 95], "tr": [278, 95], "bl": [278, 123], "br": [249, 123]}, "frameNo": 5120}, {"score": 0.9429322481155396, "boundry": {"tl": [265, 93], "tr": [295, 93], "bl": [295, 122], "br": [265, 122]}, "frameNo": 5125}, {"score": 0.9631644487380981, "boundry": {"tl": [271, 90], "tr": [300, 90], "bl": [300, 118], "br": [271, 118]}, "frameNo": 5130}, {"score": 0.9457961320877075, "boundry": {"tl": [269, 99], "tr": [298, 99], "bl": [298, 126], "br": [269, 126]}, "frameNo": 5135}, {"score": 0.9548978805541992, "boundry": {"tl": [258, 97], "tr": [286, 97], "bl": [286, 125], "br": [258, 125]}, "frameNo": 5140}, {"score": 0.9631873965263367, "boundry": {"tl": [251, 92], "tr": [281, 92], "bl": [281, 121], "br": [251, 121]}, "frameNo": 5145}, {"score": 0.9581335783004761, "boundry": {"tl": [254, 96], "tr": [283, 96], "bl": [283, 126], "br": [254, 126]}, "frameNo": 5150}, {"score": 0.9617497324943542, "boundry": {"tl": [262, 102], "tr": [293, 102], "bl": [293, 132], "br": [262, 132]}, "frameNo": 5155}, {"score": 0.9612778425216675, "boundry": {"tl": [269, 103], "tr": [298, 103], "bl": [298, 133], "br": [269, 133]}, "frameNo": 5160}, {"score": 0.9726897478103638, "boundry": {"tl": [268, 102], "tr": [297, 102], "bl": [297, 132], "br": [268, 132]}, "frameNo": 5165}, {"score": 0.9605276584625244, "boundry": {"tl": [256, 107], "tr": [286, 107], "bl": [286, 136], "br": [256, 136]}, "frameNo": 5170}, {"score": 0.9784114360809326, "boundry": {"tl": [241, 110], "tr": [270, 110], "bl": [270, 140], "br": [241, 140]}, "frameNo": 5175}, {"score": 0.9660530686378479, "boundry": {"tl": [236, 97], "tr": [266, 97], "bl": [266, 128], "br": [236, 128]}, "frameNo": 5180}, {"score": 0.9553115367889404, "boundry": {"tl": [238, 99], "tr": [269, 99], "bl": [269, 129], "br": [238, 129]}, "frameNo": 5185}, {"score": 0.9584997892379761, "boundry": {"tl": [243, 101], "tr": [273, 101], "bl": [273, 132], "br": [243, 132]}, "frameNo": 5190}, {"score": 0.958211362361908, "boundry": {"tl": [247, 98], "tr": [279, 98], "bl": [279, 129], "br": [247, 129]}, "frameNo": 5195}, {"score": 0.9738929867744446, "boundry": {"tl": [251, 97], "tr": [283, 97], "bl": [283, 130], "br": [251, 130]}, "frameNo": 5200}, {"score": 0.9369729161262512, "boundry": {"tl": [241, 105], "tr": [274, 105], "bl": [274, 138], "br": [241, 138]}, "frameNo": 5205}, {"score": 0.9531822204589844, "boundry": {"tl": [220, 102], "tr": [252, 102], "bl": [252, 134], "br": [220, 134]}, "frameNo": 5210}, {"score": 0.9759570360183716, "boundry": {"tl": [216, 98], "tr": [249, 98], "bl": [249, 131], "br": [216, 131]}, "frameNo": 5215}, {"score": 0.9751725792884827, "boundry": {"tl": [229, 101], "tr": [261, 101], "bl": [261, 133], "br": [229, 133]}, "frameNo": 5220}, {"score": 0.9600238800048828, "boundry": {"tl": [236, 105], "tr": [269, 105], "bl": [269, 139], "br": [236, 139]}, "frameNo": 5225}, {"score": 0.9715213775634766, "boundry": {"tl": [240, 109], "tr": [274, 109], "bl": [274, 143], "br": [240, 143]}, "frameNo": 5230}, {"score": 0.9795517325401306, "boundry": {"tl": [246, 101], "tr": [280, 101], "bl": [280, 136], "br": [246, 136]}, "frameNo": 5235}, {"score": 0.98310387134552, "boundry": {"tl": [245, 105], "tr": [281, 105], "bl": [281, 140], "br": [245, 140]}, "frameNo": 5240}, {"score": 0.9788936972618103, "boundry": {"tl": [236, 104], "tr": [271, 104], "bl": [271, 139], "br": [236, 139]}, "frameNo": 5245}, {"score": 0.9844807386398315, "boundry": {"tl": [238, 94], "tr": [274, 94], "bl": [274, 129], "br": [238, 129]}, "frameNo": 5250}, {"score": 0.9795984029769897, "boundry": {"tl": [241, 93], "tr": [278, 93], "bl": [278, 128], "br": [241, 128]}, "frameNo": 5255}, {"score": 0.9803842902183533, "boundry": {"tl": [241, 98], "tr": [279, 98], "bl": [279, 136], "br": [241, 136]}, "frameNo": 5260}, {"score": 0.9811151027679443, "boundry": {"tl": [244, 101], "tr": [280, 101], "bl": [280, 138], "br": [244, 138]}, "frameNo": 5265}, {"score": 0.9845209717750549, "boundry": {"tl": [250, 95], "tr": [288, 95], "bl": [288, 132], "br": [250, 132]}, "frameNo": 5270}, {"score": 0.9876763224601746, "boundry": {"tl": [246, 98], "tr": [285, 98], "bl": [285, 136], "br": [246, 136]}, "frameNo": 5275}, {"score": 0.9780346751213074, "boundry": {"tl": [232, 104], "tr": [273, 104], "bl": [273, 143], "br": [232, 143]}, "frameNo": 5280}, {"score": 0.9909811615943909, "boundry": {"tl": [219, 100], "tr": [261, 100], "bl": [261, 138], "br": [219, 138]}, "frameNo": 5285}, {"score": 0.9869784116744995, "boundry": {"tl": [228, 100], "tr": [268, 100], "bl": [268, 139], "br": [228, 139]}, "frameNo": 5290}, {"score": 0.9839566946029663, "boundry": {"tl": [228, 105], "tr": [269, 105], "bl": [269, 144], "br": [228, 144]}, "frameNo": 5295}, {"score": 0.9873521327972412, "boundry": {"tl": [230, 113], "tr": [271, 113], "bl": [271, 152], "br": [230, 152]}, "frameNo": 5300}, {"score": 0.9924057722091675, "boundry": {"tl": [231, 103], "tr": [273, 103], "bl": [273, 143], "br": [231, 143]}, "frameNo": 5305}, {"score": 0.9917368292808533, "boundry": {"tl": [232, 105], "tr": [273, 105], "bl": [273, 146], "br": [232, 146]}, "frameNo": 5310}, {"score": 0.9895670413970947, "boundry": {"tl": [223, 102], "tr": [267, 102], "bl": [267, 144], "br": [223, 144]}, "frameNo": 5315}, {"score": 0.9938334226608276, "boundry": {"tl": [215, 103], "tr": [258, 103], "bl": [258, 145], "br": [215, 145]}, "frameNo": 5320}, {"score": 0.9930988550186157, "boundry": {"tl": [216, 98], "tr": [259, 98], "bl": [259, 141], "br": [216, 141]}, "frameNo": 5325}, {"score": 0.994124710559845, "boundry": {"tl": [220, 103], "tr": [264, 103], "bl": [264, 145], "br": [220, 145]}, "frameNo": 5330}, {"score": 0.993608832359314, "boundry": {"tl": [232, 110], "tr": [275, 110], "bl": [275, 153], "br": [232, 153]}, "frameNo": 5335}, {"score": 0.9954432845115662, "boundry": {"tl": [236, 118], "tr": [282, 118], "bl": [282, 161], "br": [236, 161]}, "frameNo": 5340}, {"score": 0.9924771785736084, "boundry": {"tl": [240, 108], "tr": [286, 108], "bl": [286, 153], "br": [240, 153]}, "frameNo": 5345}, {"score": 0.9930586814880371, "boundry": {"tl": [244, 104], "tr": [288, 104], "bl": [288, 149], "br": [244, 149]}, "frameNo": 5350}, {"score": 0.9946266412734985, "boundry": {"tl": [246, 107], "tr": [293, 107], "bl": [293, 151], "br": [246, 151]}, "frameNo": 5355}, {"score": 0.9926427006721497, "boundry": {"tl": [242, 108], "tr": [289, 108], "bl": [289, 153], "br": [242, 153]}, "frameNo": 5360}, {"score": 0.9942259192466736, "boundry": {"tl": [240, 107], "tr": [285, 107], "bl": [285, 151], "br": [240, 151]}, "frameNo": 5365}, {"score": 0.9948830604553223, "boundry": {"tl": [235, 104], "tr": [282, 104], "bl": [282, 148], "br": [235, 148]}, "frameNo": 5370}, {"score": 0.9952901601791382, "boundry": {"tl": [235, 100], "tr": [284, 100], "bl": [284, 146], "br": [235, 146]}, "frameNo": 5375}, {"score": 0.995097815990448, "boundry": {"tl": [244, 103], "tr": [290, 103], "bl": [290, 149], "br": [244, 149]}, "frameNo": 5380}, {"score": 0.9953775405883789, "boundry": {"tl": [245, 106], "tr": [292, 106], "bl": [292, 152], "br": [245, 152]}, "frameNo": 5385}, {"score": 0.9935120344161987, "boundry": {"tl": [246, 106], "tr": [294, 106], "bl": [294, 151], "br": [246, 151]}, "frameNo": 5390}, {"score": 0.9950765371322632, "boundry": {"tl": [247, 101], "tr": [296, 101], "bl": [296, 146], "br": [247, 146]}, "frameNo": 5395}, {"score": 0.9961118698120117, "boundry": {"tl": [252, 100], "tr": [300, 100], "bl": [300, 146], "br": [252, 146]}, "frameNo": 5400}, {"score": 0.9942253828048706, "boundry": {"tl": [255, 101], "tr": [303, 101], "bl": [303, 147], "br": [255, 147]}, "frameNo": 5405}, {"score": 0.9956993460655212, "boundry": {"tl": [258, 97], "tr": [306, 97], "bl": [306, 144], "br": [258, 144]}, "frameNo": 5410}, {"score": 0.9937477707862854, "boundry": {"tl": [266, 93], "tr": [316, 93], "bl": [316, 140], "br": [266, 140]}, "frameNo": 5415}, {"score": 0.994424045085907, "boundry": {"tl": [264, 88], "tr": [314, 88], "bl": [314, 134], "br": [264, 134]}, "frameNo": 5420}, {"score": 0.9934325218200684, "boundry": {"tl": [265, 90], "tr": [316, 90], "bl": [316, 137], "br": [265, 137]}, "frameNo": 5425}, {"score": 0.9949465990066528, "boundry": {"tl": [290, 102], "tr": [341, 102], "bl": [341, 150], "br": [290, 150]}, "frameNo": 5430}, {"score": 0.9926977753639221, "boundry": {"tl": [318, 103], "tr": [371, 103], "bl": [371, 152], "br": [318, 152]}, "frameNo": 5435}, {"score": 0.9942209124565125, "boundry": {"tl": [353, 90], "tr": [411, 90], "bl": [411, 140], "br": [353, 140]}, "frameNo": 5440}, {"score": 0.9945903420448303, "boundry": {"tl": [384, 76], "tr": [447, 76], "bl": [447, 130], "br": [384, 130]}, "frameNo": 5445}, {"score": 0.9950498938560486, "boundry": {"tl": [409, 62], "tr": [479, 62], "bl": [479, 122], "br": [409, 122]}, "frameNo": 5450}, {"score": 0.9953141808509827, "boundry": {"tl": [413, 50], "tr": [479, 50], "bl": [479, 109], "br": [413, 109]}, "frameNo": 5455}, {"score": 0.9826891422271729, "boundry": {"tl": [421, 36], "tr": [478, 36], "bl": [478, 99], "br": [421, 99]}, "frameNo": 5460}, {"score": 0.9196930527687073, "boundry": {"tl": [436, 15], "tr": [480, 15], "bl": [480, 78], "br": [436, 78]}, "frameNo": 5465}], "Subway": [{"score": 0.7707034945487976, "boundry": {"tl": [81, 201], "tr": [170, 201], "bl": [170, 225], "br": [81, 225]}, "frameNo": 1910}, {"score": 0.7132549285888672, "boundry": {"tl": [98, 196], "tr": [183, 196], "bl": [183, 220], "br": [98, 220]}, "frameNo": 1915}, {"score": 0.6993291974067688, "boundry": {"tl": [73, 212], "tr": [162, 212], "bl": [162, 236], "br": [73, 236]}, "frameNo": 1920}, {"score": 0.7024753093719482, "boundry": {"tl": [69, 227], "tr": [161, 227], "bl": [161, 252], "br": [69, 252]}, "frameNo": 1925}, {"score": 0.77215576171875, "boundry": {"tl": [52, 222], "tr": [148, 222], "bl": [148, 251], "br": [52, 251]}, "frameNo": 1930}, {"score": 0.7498601675033569, "boundry": {"tl": [31, 223], "tr": [133, 223], "bl": [133, 250], "br": [31, 250]}, "frameNo": 1935}, {"score": 0.7369775772094727, "boundry": {"tl": [41, 225], "tr": [144, 225], "bl": [144, 251], "br": [41, 251]}, "frameNo": 1940}, {"score": 0.7713338136672974, "boundry": {"tl": [45, 217], "tr": [146, 217], "bl": [146, 243], "br": [45, 243]}, "frameNo": 1945}, {"score": 0.7751271724700928, "boundry": {"tl": [27, 191], "tr": [130, 191], "bl": [130, 217], "br": [27, 217]}, "frameNo": 1950}, {"score": 0.7446613907814026, "boundry": {"tl": [13, 178], "tr": [118, 178], "bl": [118, 204], "br": [13, 204]}, "frameNo": 1955}, {"score": 0.7556673288345337, "boundry": {"tl": [117, 169], "tr": [200, 169], "bl": [200, 194], "br": [117, 194]}, "frameNo": 1960}, {"score": 0.7773053050041199, "boundry": {"tl": [96, 159], "tr": [185, 159], "bl": [185, 185], "br": [96, 185]}, "frameNo": 1965}, {"score": 0.7790489196777344, "boundry": {"tl": [62, 151], "tr": [157, 151], "bl": [157, 182], "br": [62, 182]}, "frameNo": 1970}, {"score": 0.7355599403381348, "boundry": {"tl": [36, 150], "tr": [132, 150], "bl": [132, 182], "br": [36, 182]}, "frameNo": 1975}, {"score": 0.7062610387802124, "boundry": {"tl": [22, 155], "tr": [120, 155], "bl": [120, 184], "br": [22, 184]}, "frameNo": 1980}, {"score": 0.6870225667953491, "boundry": {"tl": [3, 162], "tr": [103, 162], "bl": [103, 192], "br": [3, 192]}, "frameNo": 1985}, {"score": 0.6715790629386902, "boundry": {"tl": [0, 151], "tr": [97, 151], "bl": [97, 179], "br": [0, 179]}, "frameNo": 1990}, {"score": 0.6938707232475281, "boundry": {"tl": [1, 154], "tr": [99, 154], "bl": [99, 182], "br": [1, 182]}, "frameNo": 1995}, {"score": 0.6980226039886475, "boundry": {"tl": [57, 156], "tr": [140, 156], "bl": [140, 177], "br": [57, 177]}, "frameNo": 2030}, {"score": 0.825118362903595, "boundry": {"tl": [123, 174], "tr": [198, 174], "bl": [198, 194], "br": [123, 194]}, "frameNo": 2035}, {"score": 0.8917880654335022, "boundry": {"tl": [0, 23], "tr": [269, 23], "bl": [269, 79], "br": [0, 79]}, "frameNo": 2040}, {"score": 0.9315787553787231, "boundry": {"tl": [19, 39], "tr": [298, 39], "bl": [298, 96], "br": [19, 96]}, "frameNo": 2045}, {"score": 0.939751148223877, "boundry": {"tl": [44, 52], "tr": [318, 52], "bl": [318, 116], "br": [44, 116]}, "frameNo": 2050}, {"score": 0.7256248593330383, "boundry": {"tl": [149, 227], "tr": [218, 227], "bl": [218, 248], "br": [149, 248]}, "frameNo": 2055}, {"score": 0.9440035223960876, "boundry": {"tl": [82, 45], "tr": [355, 45], "bl": [355, 126], "br": [82, 126]}, "frameNo": 2060}, {"score": 0.8090231418609619, "boundry": {"tl": [247, 222], "tr": [319, 222], "bl": [319, 248], "br": [247, 248]}, "frameNo": 2065}, {"score": 0.9234293699264526, "boundry": {"tl": [100, 44], "tr": [391, 44], "bl": [391, 128], "br": [100, 128]}, "frameNo": 2070}, {"score": 0.951227068901062, "boundry": {"tl": [128, 36], "tr": [400, 36], "bl": [400, 118], "br": [128, 118]}, "frameNo": 2075}, {"score": 0.8964089155197144, "boundry": {"tl": [110, 35], "tr": [405, 35], "bl": [405, 120], "br": [110, 120]}, "frameNo": 2080}, {"score": 0.7511666417121887, "boundry": {"tl": [284, 208], "tr": [356, 208], "bl": [356, 231], "br": [284, 231]}, "frameNo": 2085}, {"score": 0.7572752833366394, "boundry": {"tl": [102, 28], "tr": [408, 28], "bl": [408, 116], "br": [102, 116]}, "frameNo": 2090}, {"score": 0.8019816875457764, "boundry": {"tl": [281, 208], "tr": [352, 208], "bl": [352, 231], "br": [281, 231]}, "frameNo": 2095}, {"score": 0.7883803844451904, "boundry": {"tl": [201, 211], "tr": [268, 211], "bl": [268, 233], "br": [201, 233]}, "frameNo": 2100}, {"score": 0.9181384444236755, "boundry": {"tl": [100, 31], "tr": [386, 31], "bl": [386, 112], "br": [100, 112]}, "frameNo": 2105}, {"score": 0.8279241323471069, "boundry": {"tl": [248, 202], "tr": [317, 202], "bl": [317, 225], "br": [248, 225]}, "frameNo": 2110}, {"score": 0.8699147701263428, "boundry": {"tl": [150, 202], "tr": [217, 202], "bl": [217, 222], "br": [150, 222]}, "frameNo": 2115}, {"score": 0.8528022766113281, "boundry": {"tl": [13, 30], "tr": [333, 30], "bl": [333, 92], "br": [13, 92]}, "frameNo": 2120}, {"score": 0.927307665348053, "boundry": {"tl": [42, 28], "tr": [322, 28], "bl": [322, 86], "br": [42, 86]}, "frameNo": 2125}, {"score": 0.8830807209014893, "boundry": {"tl": [5, 15], "tr": [313, 15], "bl": [313, 78], "br": [5, 78]}, "frameNo": 2130}, {"score": 0.918084979057312, "boundry": {"tl": [7, 17], "tr": [292, 17], "bl": [292, 75], "br": [7, 75]}, "frameNo": 2135}, {"score": 0.8775596618652344, "boundry": {"tl": [0, 2], "tr": [268, 2], "bl": [268, 69], "br": [0, 69]}, "frameNo": 2140}, {"score": 0.8402780294418335, "boundry": {"tl": [28, 170], "tr": [108, 170], "bl": [108, 191], "br": [28, 191]}, "frameNo": 2145}, {"score": 0.7179707288742065, "boundry": {"tl": [93, 161], "tr": [168, 161], "bl": [168, 181], "br": [93, 181]}, "frameNo": 2150}, {"score": 0.6967829465866089, "boundry": {"tl": [73, 152], "tr": [150, 152], "bl": [150, 173], "br": [73, 173]}, "frameNo": 2155}, {"score": 0.7278605699539185, "boundry": {"tl": [60, 146], "tr": [139, 146], "bl": [139, 168], "br": [60, 168]}, "frameNo": 2160}, {"score": 0.724624752998352, "boundry": {"tl": [50, 144], "tr": [129, 144], "bl": [129, 166], "br": [50, 166]}, "frameNo": 2165}, {"score": 0.7119631171226501, "boundry": {"tl": [38, 141], "tr": [118, 141], "bl": [118, 162], "br": [38, 162]}, "frameNo": 2170}]}'

detector.loadJson(ij)
detector.detect()

with open("d1.txt", "w") as text_file:
    text_file.write(detector.toJson())

with open("d1_final.txt", "w") as text_file:
    text_file.write(detector.toFinalJson())

# print(detector.toJson())
# print(detector.toFinalJson())

def play_video(detector):
    # window name and size
    detector.fillFrames()
    cv.namedWindow("video", cv.WINDOW_AUTOSIZE)
    i=5000
    while i < 9000:
        # Read video capture
        frame = detector.getFrame(i)
        frame = cv.cvtColor(frame,cv.COLOR_RGB2BGR)
        # img = im.fromarray(frame)
        # Display each frame
        cv.imshow("video", frame)
        # show one frame at a time
        key = cv.waitKey(00)
        # Quit when 'q' is pressed
        if key == ord('q'):
            break
        i+=1
    # Exit and distroy all windows
    cv.destroyAllWindows()

play_video(detector)




