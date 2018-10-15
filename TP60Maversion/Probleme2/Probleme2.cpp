// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)
#define F_CPU 8000000
#define BAUD 2400
#include <util/setbaud.h>
#include <avr/interrupt.h>
#include <avr/io.h>
#include "can.h"

#include <util/delay.h>
volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0; 
volatile uint8_t compteur = 0;
uint8_t rouge = 0b10; //Led prend la couleur rouge	
uint8_t vert = 0b01; //Led prend la couleur verte

//% serieViaUSB -l




void ambree()
{
	PORTB =rouge;
    _delay_ms(5);
    PORTB =vert;
    _delay_ms(10);
	}

int main()
{


    DDRA = 0x00; // PORT A est en mode entree
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    
    can photo;
    
    uint16_t result;
   for(;;){
   
    result = (uint8_t)photo.lecture(0)>>2; //Lecture obtenu sur 10 bits
    //result >> 2;//On elimine les 2 bits les moins significatifs avec un decalage
    
   //uint8_t result2;
   // result = (uint8_t)result; //On converti le resultat de 16 bits en 8 bits
   
    _delay_ms(10);
    if (result < 40)
        PORTB = vert;
    
    else if(result >= 100 && result < 180)
    {//couleur ambre
         
           ambree();
         
         }
     else if (result >= 190)
		PORTB = rouge;
    }

	return 0;

}


    


