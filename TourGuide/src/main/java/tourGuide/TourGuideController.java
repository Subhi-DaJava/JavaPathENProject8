package tourGuide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import gpsUtil.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;

@RestController
public class TourGuideController {
    private static final Logger logger = LoggerFactory.getLogger(TourGuideService.class);

    private final TourGuideService tourGuideService;

    private final RewardsService rewardsService;
    @Autowired
    public TourGuideController(TourGuideService tourGuideService, RewardsService rewardsService) {
        this.tourGuideService = tourGuideService;
        this.rewardsService = rewardsService;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping(value = "/getLocation", produces = "application/json")
    public String getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        logger.info("Get successfully the location of an user, username: {}, from TourGuideController", userName);
        return JsonStream.serialize(visitedLocation.location);
    }

    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        logger.debug("getNearByAttractions starts here.");
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));

        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }

 /*   @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }*/
    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
        List<UserReward> userRewards = tourGuideService.getUserRewards(getUser(userName));
       return userRewards;
    }

    @RequestMapping(value = "/getAllCurrentLocations", produces = "application/json")
    public String getAllCurrentLocations() {
        logger.debug("getAllCurrentLocations method starts here, form TourGuideController");

        Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();
        logger.info("AllCurrentLocations({} total) have been retrieved successfully, from TourGuideController", allCurrentLocations.size());
        return  JsonStream.serialize(allCurrentLocations);
    }

  /*  @RequestMapping( "/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }
*/
       @RequestMapping("/getTripDeals")
        public List<Provider> getTripDeals(@RequestParam String userName) {
            List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
            return providers;
        }
    @RequestMapping("/users/user")
    private User getUser(@RequestParam String userName) {
        User userByUserName =  tourGuideService.getUser(userName);
        logger.info("User is successfully retrieved by username: {}, from TourGuideController", userName);
        return userByUserName;
    }

    @RequestMapping("/users")
    public List<User> getAllUsers() {
        List<User> users = tourGuideService.getAllUsers();
        logger.info("All users ({} total) successfully loaded, from TourGuideController", users.size());
        return users;
    }
    @PostMapping("/users")
    public void addUser(@RequestBody User user) {
        tourGuideService.addUser(user);
        logger.info("New User successfully added, username:{}, from TourGuideController", user.getUserName());
    }
    @GetMapping("/users/user/{userId}")
    public User getUserByUserId(@PathVariable String userId) {
        logger.info("User is successfully retrieved by UserId:{} form TourGuideController", userId);
        return tourGuideService.getUserByUserId(userId);
    }

    @RequestMapping("/getAllTripDeals")
    public Map<String, List<Provider>> getAllTripDeals() {
        logger.debug("getAllTripDeals starts now!");
        List<User> users = getAllUsers();
        Map<String, List<Provider>> allProviders = new HashMap<>();

        users.parallelStream().forEach(user -> allProviders.put(user.getUserName(), tourGuideService.getTripDeals(getUser(user.getUserName()))));
        logger.debug("AllTripDeals have been successfully loaded!");
        return allProviders;
    }

    @RequestMapping("/allRewards")
    public Map<String, List<UserReward>> getAllRewards() {
        logger.debug("getAllRewards starts now!");
        List<User> users = getAllUsers();
        Map<String, List<UserReward>> allUserRewards = new HashMap<>();

        users.parallelStream().forEach(rewardsService::calculateRewards);
        users.parallelStream().forEach(user -> {
            List<UserReward> userRewards = user.getUserRewards();
            allUserRewards.put(user.getUserName(), userRewards);
        });
        logger.debug("AllRewards have been successfully loaded!");
        return allUserRewards;
    }

}