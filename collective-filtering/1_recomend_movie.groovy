/**
 * Created by aman on 1/25/14.
 *
 *
 * This program absolutely relies on the given data file and its known format.
 */
import kcl.waterloo.marker.GJMarker
import kcl.waterloo.plot.WPlot
import java.awt.Color

import static org.math.array.Matrix.*
import static org.math.plot.Plot.*

movies = []
users = []
userRatings = [:]

new File('Movie_Ratings.csv').withReader { reader ->
    users = reader.readLine().split(',')[1..-1]
    (0..users.size()-1).each { userRatings[it] = [] }

    reader.splitEachLine(','){ cells ->
        movies += cells[0]
        cells[1..-1].eachWithIndex { def rating, int i ->
            try{userRatings[i] += rating as int}
            catch(all){userRatings[i] += 0}
        }
    }
}

String name = args.size()>0 ? args[0] : "Josh"
int minRating = args.size()>1 ? args[1] as int : 4


recommendedMoviesForUser(name, minRating)

nearestNeighbors(users.findIndexOf {it=~name}).each { k, v ->
    println users[k] + ":" + v
}


def recommendedMoviesForUser(String userName, int minRating = 0){
    def userIndex = users.findIndexOf { it =~ userName}
    if(userIndex < 0){println "User not found"; return;}

    print "Recommendation for ${userName.padRight(12)} "

    def myTopMovies = topRatedMoviesByUser(userIndex, 1)
    def sortedNeighbors = nearestNeighbors(userIndex)

    int neighborDistance = 1
    for ( neighbor in sortedNeighbors ){
        def topMoviesByNearestNeighbor = topRatedMoviesByUser(neighbor.key, minRating)

        // recommendation are the movies not rated by me but by my nearest neighbor
        def recommendation = topMoviesByNearestNeighbor.keySet() - myTopMovies.keySet().intersect(topMoviesByNearestNeighbor.keySet())

        if(recommendation.size()){
            print " by ${users[neighbor.key].padRight(11)} ( ${neighbor.value.round(2)}, $neighborDistance) :"
            recommendation.each {
                print "${it[0]} (${movieRatingByUser(it[0],neighbor.key)}☆ ) "
            }

            println "\n"
            print "$userName's top movies:"
            myTopMovies.sort{it.key[0]}.each { k,v -> print " ${k[0]} ($v)"}
            println "\n"
            print "recomender's top movies:"
            topMoviesByNearestNeighbor.sort{it.key[0]}.each { k,v -> print " ${k[0]} ($v)"}
            println "\n"
            println "Movies $userName hasn't seen yet: $recommendation"
            break;
        }
        neighborDistance++
    }

    println ""
}

def topRatedMoviesByUser(int userIndex, int minRating = 0){
    def sortedMovies = [:]

    userRatings[userIndex].eachWithIndex{ def rating, int i ->
        if(rating >= minRating){
            sortedMovies[[movies[i]]] = rating
        }
    }

    def tmp = [:]
    sortedMovies.sort{it.value}.reverseEach {tmp[it.key] = it.value}
    return tmp;
}


def nearestNeighbors(int userIndex){
    def sortedNeighbors = [:]
    def myRating = userRatings[userIndex]
    userRatings.eachWithIndex{ def rating, int i ->
        if(i != userIndex){
            sortedNeighbors[i] = manhattanDistance(myRating, userRatings[i])
        }
    }
    sortedNeighbors.sort { it.value }
}

// only consider the movies which has been rated by both the users
def manhattanDistance(def userRatingsA, def userRatingsB){
    double distance = 0
    def count = 0
    (0..Math.max(userRatingsA.size(), userRatingsB.size())-1).each { i ->
        if(userRatingsA[i] && userRatingsB[i]){
            distance += (userRatingsA[i] - userRatingsB[i]).abs()
            count++
        }
    }

    return count > 0? distance / count :Integer.MAX_VALUE
}

// only consider the movies which has been rated by both the users
def euclideanDistance(def userRatingsA, def userRatingsB){
    double distance = 0;
    def count = 0
    (0..Math.max(userRatingsA.size(), userRatingsB.size())-1).each { i ->
        if(userRatingsA[i] && userRatingsB[i]){
            distance += (userRatingsA[i] - userRatingsB[i])*(userRatingsA[i] - userRatingsB[i])
            count++
        }
    }

    return count > 0? Math.sqrt(distance)/count : Integer.MAX_VALUE
}

def movieRatingByUser(movieName, userIndex){
    def index = movies.findIndexOf {it==~movieName}
    if(index){
        return userRatings[userIndex][index]
    }
    return 0
}



def compareUserRatings(user1, user2){
    def u1 = users.findIndexOf {it =~ user1}
    def u2 = users.findIndexOf {it =~ user2}

    compareUserRatingsByID(u1, u2)
}

def compareUserRatingsByID(u1, u2){

    def w1=WPlot.scatter('XData': 0..23,
            'YData': userRatings[u1][0..23],
            'Marker': GJMarker.Circle(5),
            'Fill': Color.blue)
    def w2=WPlot.line('XData': 0..23,
            'YData': userRatings[u1],
            'LineColor': "BLUE")
    w1+=w2

    def w3=WPlot.scatter('XData':0..23,
            'YData': userRatings[u2],
            'Marker': GJMarker.Square(5),
            'Fill': Color.green)
    def w4=WPlot.line('XData': 0..23,
            'YData': userRatings[u2][0..23],
            'LineColor': "GREEN")
    w3+=w4
    def f=w1.createFrame()
    w1.getPlot().getParentGraph() + w3.getPlot()
    w1.getPlot().getParentGraph().autoScale()

    return w1
}



def plotNeighborEdges(userName){
    def me = users.findIndexOf { it =~ userName }
    def list = nearestNeighbors( me )
    def first =  list.keySet().first()
    def last =  list.keySet().last()

    def data = (0..users.size()-1).collect { 0 }
    userRatings[first].eachWithIndex { def entry, int i -> data[i] = entry}

    def w1=WPlot.scatter('XData': 0..data.size()-1,
            'YData': data,
            'Marker': GJMarker.Circle(5),
            'Fill': Color.green)
    def w2=WPlot.line('XData': 0..data.size()-1,
            'YData': data,
            'LineColor': "GREEN")
    w1+=w2

    data = ((0..users.size()-1).collect { 0 })
    userRatings[last].eachWithIndex { def entry, int i -> data[i] = entry}
    def w3=WPlot.scatter('XData':0..data.size()-1,
            'YData': data,
            'Marker': GJMarker.Square(5),
            'Fill': Color.red)
    def w4=WPlot.line('XData': 0..data.size()-1,
            'YData': data,
            'LineColor': "RED")
    w3+=w4

    data = ((0..users.size()-1).collect { 0 })
    userRatings[me].eachWithIndex { def entry, int i -> data[i] = entry}
    def w5=WPlot.scatter('XData':0..data.size()-1,
            'YData': data,
            'Marker': GJMarker.Square(5),
            'Fill': Color.blue)
    def w6=WPlot.line('XData': 0..data.size()-1,
            'YData': data,
            'LineColor': "BLUE")

    w5+=w6

    def f=w1.createFrame()
    w1.getPlot().getParentGraph() + w3.getPlot()
    w1.getPlot().getParentGraph() + w5.getPlot()
    w1.getPlot().getParentGraph().autoScale()

    return w1
}
def plotNeighborEdges1(userName){
    def me = users.findIndexOf { it =~ userName }
    def list = nearestNeighbors( me )
    def first =  list.keySet().first()
    def last =  list.keySet().last()

    def data = (0..users.size()-1).collect { [0,0] }

    userRatings[first].eachWithIndex { def entry, int i ->
        data[i][0] =i
        data[i][1] = entry
    }

//    println data

    def A = matrix(data)
    println A
    plot("first",A,"LINE")


    data = (0..users.size()-1).collect { [0,0] }
    userRatings[last].eachWithIndex { def entry, int i ->
        data[i][0] =i
        data[i][1] = entry
    }

//    println data

    A = matrix(data)
    println A
    plot("last",A,"LINE")

    data = (0..users.size()-1).collect { [0,0] }
    userRatings[me].eachWithIndex { def entry, int i ->
        data[i][0] =i
        data[i][1] = entry
    }

//    println data

    A = matrix(data)
    println A
    plot("ME",A,"LINE")
}