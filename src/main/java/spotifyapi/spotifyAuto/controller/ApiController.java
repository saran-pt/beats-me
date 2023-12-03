package spotifyapi.spotifyAuto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import spotifyapi.spotifyAuto.service.SpotifyService;

@RestController
public class ApiController{

    @Autowired
    SpotifyService spotifyService;
    @GetMapping("/")
    public RedirectView Home() {
        return new RedirectView(spotifyService.authenticate());
    }

    @GetMapping("/callback")
    public String callBack(@RequestParam String code) {
        return spotifyService.callBack(code);
    }
}
