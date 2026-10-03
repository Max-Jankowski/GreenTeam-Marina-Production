/*
Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Moffat Bay Marina Project
The Green Team
CSD460
/*

/* 
Max Jankowski 
Bellevue University 
CSD 460
Green Team Moffate bay project 
prototype data that i sused for the prototype slip availibity
file stays in local storage for while we dont have an active DB ready. 
*/

const SLIP_INVENTORY={26:30,40:24,50:18};
const ELECTRIC_FEE=10;
const RATE_PER_FOOT=10;

const defaultMarinaState={
 reserved:{26:26,40:24,50:12},   // created to show what a full bookeed section looks like 
 waitlist:{26:0,40:3,50:1}
};

function getMarinaState(){
 const x=localStorage.getItem("moffatBayMarinaState");
 return x?JSON.parse(x):structuredClone(defaultMarinaState);
}
function saveMarinaState(s){localStorage.setItem("moffatBayMarinaState",JSON.stringify(s))}
function resetMarinaState(){localStorage.removeItem("moffatBayMarinaState")}

function availableSlips(size){const s=getMarinaState();return SLIP_INVENTORY[size]-s.reserved[size]}

// Customer need to reserve the smallest slip that fits their vessel so 34 ft boat take a 40 ft slip).
function requiredSlipSize(boatLength){
 if(boatLength<=0||isNaN(boatLength))return null;
 if(boatLength<=26)return 26;
 if(boatLength<=40)return 40;
 if(boatLength<=50)return 50;
 return null; // if longer than our largest slip
}

// Fee calculation for monthly cost = $10 per foot of boat + $10 electric (34 ft boat = $350). Let me know if a goofed here. I think i got the project overveiw correct 
function monthlyCost(boatLength){return boatLength*RATE_PER_FOOT+ELECTRIC_FEE}

function joinWaitlist(size){const s=getMarinaState();s.waitlist[size]++;saveMarinaState(s);return s.waitlist[size]}
